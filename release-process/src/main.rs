use anyhow::anyhow;
use anyhow::Result;
use git2::Repository;
use git_cliff::args::Opt;
use git_conventional::Type;
use regex::Captures;
use regex::Regex;
use std::fmt::Display;
use std::fs::File;
use std::io::Read;
use std::io::Write;
use std::path::PathBuf;
use std::process;

const PROJECT_DIR: &str = env!("CARGO_MANIFEST_DIR");
const VERSION_NAME_REGEX: &str = r#"(versionName ")([0-9]+\.[0-9]+\.[0-9]+)(")"#;
const VERSION_CODE_REGEX: &str = r#"(versionCode )([0-9]+)"#;

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy)]
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

    let last_version = last_version(&repository)?;

    println!("Latest version: {last_version}");

    let next_version = next_version(&repository, &last_version)?;

    drop(repository);

    if let Some(next_version) = next_version {
        println!("Next version: {next_version}");

        update_versions_in_build_gradle(&next_version)?;

        changelog(&next_version)?;
    }

    Ok(())
}

fn last_version(repository: &Repository) -> Result<Version> {
    let tags: Result<Vec<Version>> = repository
        .tag_names(None)
        .expect("Couldn't get tags")
        .iter()
        .filter_map(|tag| tag.map(Version::from_tag))
        .collect();
    let mut tags = tags?;
    tags.sort();

    tags.last()
        .cloned()
        .ok_or_else(|| anyhow!("Couldn't get a tag"))
}

fn next_version(repository: &Repository, last_version: &Version) -> Result<Option<Version>> {
    let last_tag = repository
        .resolve_reference_from_short_name(&last_version.as_tag())?
        .peel_to_commit()?;

    let mut commits = repository.revwalk()?;
    commits.simplify_first_parent()?;
    commits.push_head()?;
    commits.hide(last_tag.id())?;

    let mut major_bump = false;
    let mut minor_bump = false;
    let mut patch_bump = false;

    for oid in commits {
        let oid = oid?;
        let commit = repository.find_commit(oid)?;

        let message = String::from_utf8_lossy(commit.message_bytes());
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
    }

    Ok(if major_bump {
        Some(last_version.next_major())
    } else if minor_bump {
        Some(last_version.next_minor())
    } else if patch_bump {
        Some(last_version.next_patch())
    } else {
        None
    })
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

fn changelog(next_version: &Version) -> Result<()> {
    let main_config_file = PathBuf::from(PROJECT_DIR).join("../cliff.toml");

    let args = Opt {
        verbose: 0,
        config: main_config_file,
        workdir: Some(PathBuf::from(PROJECT_DIR).join("..")),
        repository: Some(PathBuf::from(PROJECT_DIR).join("..")),
        include_path: None,
        exclude_path: None,
        with_commit: None,
        prepend: None,
        output: Some(PathBuf::from(PROJECT_DIR).join("../CHANGELOG.md")),
        tag: Some(format!("{next_version}")),
        body: None,
        init: false,
        latest: false,
        current: false,
        unreleased: true,
        date_order: false,
        context: false,
        strip: None,
        sort: git_cliff::args::Sort::Oldest,
        range: None,
    };

    git_cliff::run(args)?;

    Ok(())
}
