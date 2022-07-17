use anyhow::anyhow;
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
            minor: self.minor,
            patch: self.patch,
        }
    }

    fn next_minor(&self) -> Self {
        Self {
            major: self.major,
            minor: self.minor + 1,
            patch: self.patch,
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

        update_versions_in_build_gradle(&next_version)?;

        let changelog_file = File::create(PathBuf::from(PROJECT_DIR).join("../CHANGELOG2.md"))?;
        changelog2(&repo, &next_version, false, &changelog_file)?;
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

fn update_versions_in_build_gradle(next_version: &Version) -> Result<()> {
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

    let next_version_code: u64 = (version_code_regex
        .captures(&content)
        .ok_or_else(|| anyhow!("Couldn't find version code"))?[2]
        .parse::<u64>()?)
        + 1;

    let content = version_code_regex.replace(&content, |caps: &Captures| {
        format!("{}{}", &caps[1], next_version_code)
    });

    let mut output = File::create(app_gradle_file)?;
    output.write_all(content.as_bytes())?;

    Ok(())
}

fn changelog2<W>(repo: &Repo, next_version: &Version, only_next: bool, into: W) -> Result<()>
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

    let config = Config::parse(&PathBuf::from(PROJECT_DIR).join("cliff.toml"))?;

    let changelog = Changelog::new(releases, &config)?;

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
        match to {
            Some(tag) => revwalker.hide(tag.commit_id)?,
            None => {}
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
