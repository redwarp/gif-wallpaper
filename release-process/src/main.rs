use anyhow::anyhow;
use anyhow::Context;
use anyhow::Result;
use git2::Oid;
use git2::Repository;
use git2::Revwalk;
use git_cliff::changelog::Changelog;
use git_cliff_core::commit::Commit;
use git_cliff_core::config::Config;
use git_conventional::Type;
use regex::Captures;
use regex::Regex;
use std::fmt::Display;
use std::fs::File;
use std::io::Read;
use std::io::Write;
use std::path::PathBuf;
use std::process::Command;

const PROJECT_DIR: &str = env!("CARGO_MANIFEST_DIR");
const VERSION_NAME_REGEX: &str = r#"(versionName ")([0-9]+\.[0-9]+\.[0-9]+)(")"#;
const VERSION_CODE_REGEX: &str = r#"(versionCode )([0-9]+)"#;

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Debug)]
struct Version {
    major: u64,
    minor: u64,
    patch: u64,
}

impl Version {
    fn from_tag(tag: &str) -> Result<Self> {
        let parts: Vec<_> = tag[1..].split('.').collect();
        if parts.len() != 3 {
            return Err(anyhow!("Wrong tag"));
        }

        let major = parts[0].parse()?;
        let minor = parts[1].parse()?;
        let patch = parts[2].parse()?;

        Ok(Self {
            major,
            minor,
            patch,
        })
    }

    fn as_tag(&self) -> String {
        format!("v{self}")
    }

    fn next_major(&self) -> Self {
        Self {
            major: self.major + 1,
            minor: 0,
            patch: 0,
        }
    }

    fn next_minor(&self) -> Self {
        Self {
            major: self.major,
            minor: self.minor + 1,
            patch: 0,
        }
    }

    fn next_patch(&self) -> Self {
        Self {
            major: self.major,
            minor: self.minor,
            patch: self.patch + 1,
        }
    }
}

impl Display for Version {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_fmt(format_args!(
            "{major}.{minor}.{patch}",
            major = self.major,
            minor = self.minor,
            patch = self.patch
        ))
    }
}

fn main() -> Result<()> {
    let repository =
        Repository::open(PathBuf::from(PROJECT_DIR).join("..")).expect("Couldn't find git repo");

    let repo = Repo::new(repository);
    let last_version = last_version(&repo)?;

    println!("Last version {last_version:?}");

    if let Some(next_version) = next_version(&repo)? {
        println!("Next version: {next_version:?}");
        let next_version_code = next_version_code()?;

        update_versions_in_build_gradle(&next_version, next_version_code)?;

        let changelog_file = File::create(PathBuf::from(PROJECT_DIR).join("../CHANGELOG.md"))?;

        let main_config = Config::parse(&PathBuf::from(PROJECT_DIR).join("main-cliff.toml"))?;
        changelog(&repo, &next_version, false, &main_config, &changelog_file)?;

        let fastlane_changelog_folder = PathBuf::from(PROJECT_DIR)
            .join("..")
            .join("fastlane/metadata/android/en-US/changelogs");
        std::fs::create_dir_all(&fastlane_changelog_folder)?;
        let fastlane_changelog_file =
            fastlane_changelog_folder.join(format!("{next_version_code}.txt"));

        let fastlane_config =
            Config::parse(&PathBuf::from(PROJECT_DIR).join("fastlane-cliff.toml"))?;
        changelog(
            &repo,
            &next_version,
            true,
            &fastlane_config,
            File::create(&fastlane_changelog_file)?,
        )?;

        create_commit(&repo, &next_version, next_version_code)?;
    }

    Ok(())
}

fn last_version(repo: &Repo) -> Result<Option<Version>> {
    repo.last_tag().map(|tag| tag.map(|tag| tag.version))
}

fn next_version(repo: &Repo) -> Result<Option<Version>> {
    let remove_line_break = Regex::new(r"(\w)\n(\w)")?;

    if let Some(last_tag) = repo.last_tag()? {
        let mut major_bump = false;
        let mut minor_bump = false;
        let mut patch_bump = false;
        let walker = repo.walker(None, Some(&last_tag))?;

        let mut count = 0;
        for oid in walker {
            let oid = oid?;
            let commit = repo.repository.find_commit(oid)?;

            let message = String::from_utf8_lossy(commit.message_bytes());
            // git_conventional panics when string is like "hello\nyou" so let's sanitize it.
            let message = remove_line_break.replace(&message, "$1 $2");
            let parsed = git_conventional::Commit::parse(&message);

            if let Ok(parsed) = parsed {
                if parsed.breaking() {
                    major_bump = true
                } else if parsed.type_() == Type::FEAT {
                    minor_bump = true
                } else if parsed.type_() == Type::FIX {
                    patch_bump = true
                }
            }
            count += 1;
        }

        println!("Walked through {count} commits");

        Ok(if major_bump {
            Some(last_tag.version.next_major())
        } else if minor_bump {
            Some(last_tag.version.next_minor())
        } else if patch_bump {
            Some(last_tag.version.next_patch())
        } else {
            None
        })
    } else {
        Ok(None)
    }
}

fn next_version_code() -> Result<u64> {
    let app_gradle_file = PathBuf::from(PROJECT_DIR)
        .join("..")
        .join("app/build.gradle");

    let version_code_regex = Regex::new(VERSION_CODE_REGEX)?;
    let content = std::fs::read_to_string(app_gradle_file)?;

    let next_version_code: u64 = (version_code_regex
        .captures(&content)
        .ok_or_else(|| anyhow!("Couldn't find version code"))?[2]
        .parse::<u64>()?)
        + 1;

    Ok(next_version_code)
}

fn update_versions_in_build_gradle(next_version: &Version, next_version_code: u64) -> Result<()> {
    let app_gradle_file = PathBuf::from(PROJECT_DIR)
        .join("..")
        .join("app/build.gradle");

    let mut content = String::new();
    File::open(&app_gradle_file)?.read_to_string(&mut content)?;

    let version_name_regex = Regex::new(VERSION_NAME_REGEX)?;
    let version_code_regex = Regex::new(VERSION_CODE_REGEX)?;

    let content = version_name_regex.replace(&content, |caps: &Captures| {
        format!("{}{}{}", &caps[1], next_version, &caps[3])
    });

    let content = version_code_regex.replace(&content, |caps: &Captures| {
        format!("{}{}", &caps[1], next_version_code)
    });

    let mut output = File::create(app_gradle_file)?;
    output.write_all(content.as_bytes())?;

    Ok(())
}

fn create_commit(repo: &Repo, next_version: &Version, next_version_code: u64) -> Result<()> {
    // Read https://zsiciarz.github.io/24daysofrust/book/vol2/day16.html
    // and https://paritytech.github.io/substrate/master/git2/struct.Repository.html#method.signature

    let repository = &repo.repository;

    let mut index = repository.index()?;
    index.add_path(&PathBuf::from("CHANGELOG.MD"))?;
    index.add_path(&PathBuf::from("app/build.gradle"))?;
    index.add_path(&PathBuf::from(format!(
        "fastlane/metadata/android/en-US/changelogs/{next_version_code}.txt"
    )))?;
    index.write()?;

    let message = format!("chore(release): {next_version}");
    // Here, we cheat a bit and use command line to do the actual commit:
    // We want to allow for signing the commit, and it's not trivial in pure rust.
    git(&["commit", "-m", &message])?;

    let tag_message = format!("Version {next_version}");
    git(&["tag", "-a", &next_version.as_tag(), "-m", &tag_message])?;

    Ok(())
}

fn changelog<W>(
    repo: &Repo,
    next_version: &Version,
    only_next: bool,
    config: &Config,
    into: W,
) -> Result<()>
where
    W: Write,
{
    let mut into = into;
    let head_id = repo.repository.head()?.peel_to_commit()?.id();
    let next_tag = Tag::new(*next_version, head_id);
    let releases = if only_next {
        vec![Release::new(next_tag.version, head_id, repo.last_tag()?)]
    } else {
        let mut tags = repo.tags()?;
        tags.push(next_tag);
        tags.sort_by(|a, b| a.version.cmp(&b.version));
        tags.reverse();

        let mut releases: Vec<Release> = tags
            .windows(2)
            .map(|window| {
                Release::new(
                    window[0].version,
                    window[0].commit_id,
                    Some(window[1].clone()),
                )
            })
            .collect();

        if let Some(last) = tags.pop() {
            releases.push(Release::new(last.version, last.commit_id, None));
        }

        releases
    };

    let mut releases: Vec<git_cliff_core::release::Release> = releases
        .iter()
        .flat_map(|release| release.as_cliff_release(repo))
        .collect();
    windows_mut_each(&mut releases[..], 2, |window| {
        window[1].previous = Some(Box::new(window[0].clone()))
    });
    releases.reverse();

    let changelog = Changelog::new(releases, config)?;

    changelog.generate(&mut into)?;

    Ok(())
}

struct Repo {
    repository: Repository,
}

impl Repo {
    fn new(repository: Repository) -> Self {
        Self { repository }
    }

    fn tags(&self) -> Result<Vec<Tag>> {
        let mut tags: Vec<Tag> = Vec::new();
        let tag_names = self.repository.tag_names(None)?;
        for tag_name in tag_names.iter().flatten().map(String::from) {
            let obj = self.repository.revparse_single(&tag_name)?;
            if let Ok(commit) = obj.clone().into_commit() {
                tags.push(Tag::new(Version::from_tag(&tag_name)?, commit.id()));
            } else if let Some(tag) = obj.as_tag() {
                if let Some(commit) = tag
                    .target()
                    .ok()
                    .and_then(|target| target.into_commit().ok())
                {
                    tags.push(Tag::new(Version::from_tag(&tag_name)?, commit.id()));
                }
            }
        }
        tags.sort_by(|a, b| a.version.cmp(&b.version));

        Ok(tags)
    }

    fn last_tag(&self) -> Result<Option<Tag>> {
        let mut tags = self.tags()?;
        Ok(tags.pop())
    }

    fn walker(&self, from: Option<&Tag>, to: Option<&Tag>) -> Result<Revwalk> {
        let mut revwalker = self.repository.revwalk()?;
        revwalker.simplify_first_parent()?;
        match from {
            Some(tag) => revwalker.push(tag.commit_id),
            None => revwalker.push_head(),
        }?;
        if let Some(tag) = to {
            revwalker.hide(tag.commit_id)?
        }

        Ok(revwalker)
    }
}

#[derive(Debug, Clone)]
struct Tag {
    version: Version,
    commit_id: Oid,
}

impl Tag {
    fn new(version: Version, commit_id: Oid) -> Self {
        Self { version, commit_id }
    }
}

struct Release {
    version: Version,
    from: Oid,
    to: Option<Tag>,
}

impl Release {
    fn new(version: Version, from: Oid, to: Option<Tag>) -> Self {
        Self { version, from, to }
    }

    fn as_cliff_release(&self, repo: &Repo) -> Result<git_cliff_core::release::Release> {
        let remove_line_break = Regex::new(r"(\w)\n(\w)")?;

        let walker = repo.walker(Some(&Tag::new(self.version, self.from)), self.to.as_ref())?;
        let tag_commit = repo.repository.find_commit(self.from)?;

        let mut commits: Vec<Commit> = walker
            .into_iter()
            .flatten()
            .filter_map(|commit_id| repo.repository.find_commit(commit_id).ok())
            .map(|commit| {
                let message = String::from_utf8_lossy(commit.message_bytes());
                // git_conventional panics when string is like "hello\nyou" so let's sanitize it.
                let message = remove_line_break.replace(&message, "$1 $2");
                Commit::new(commit.id().to_string(), message.into_owned())
            })
            .collect();
        commits.reverse();
        Ok(git_cliff_core::release::Release {
            version: Some(self.version.as_tag()),
            commits,
            commit_id: Some(self.from.to_string()),
            timestamp: tag_commit.time().seconds(),
            previous: None,
        })
    }
}

fn windows_mut_each<T>(v: &mut [T], n: usize, mut f: impl FnMut(&mut [T])) {
    let mut start = 0;
    let mut end = n;
    while end <= v.len() {
        f(&mut v[start..end]);
        start += 1;
        end += 1;
    }
}

// From https://github.com/MarcoIeni/release-plz/blob/main/crates/git_cmd/src/lib.rs
fn git(args: &[&str]) -> Result<String> {
    let work_dir = PathBuf::from(PROJECT_DIR).join("..");

    let args: Vec<&str> = args.iter().map(|s| s.trim()).collect();
    let output = Command::new("git")
        .arg("-C")
        .arg(&work_dir)
        .args(&args)
        .output()
        .with_context(|| {
            format!("error while running git in directory `{work_dir:?}` with args `{args:?}`")
        })?;
    let stdout = string_from_bytes(output.stdout)?;
    if output.status.success() {
        Ok(stdout)
    } else {
        let mut error = "error while running git:\n".to_string();
        if !stdout.is_empty() {
            error.push_str("- stdout: ");
            error.push_str(&stdout);
        }
        let stderr = string_from_bytes(output.stderr)?;
        if !stderr.is_empty() {
            error.push_str("- stderr: ");
            error.push_str(&stderr);
        }
        Err(anyhow!(error))
    }
}

fn string_from_bytes(bytes: Vec<u8>) -> Result<String> {
    let stdout = String::from_utf8(bytes).context("cannot extract stderr")?;
    let stdout = stdout.trim();
    Ok(stdout.to_string())
}
