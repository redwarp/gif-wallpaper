use std::fmt::Display;
use std::fs::File;
use std::io::Read;
use std::io::Write;
use std::path::PathBuf;

use anyhow::anyhow;
use anyhow::Ok;
use anyhow::Result;
use conventional_commits_next_version_lib::Commits;
use git2::Repository;
use regex::Captures;
use regex::Regex;

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

    fn as_semver(&self) -> semver::Version {
        semver::Version::new(self.major, self.minor, self.patch)
    }

    fn from_semver(version: &semver::Version) -> Self {
        Self {
            major: version.major,
            minor: version.minor,
            patch: version.patch,
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

    println!("Next version: {next_version}");

    update_version_in_build_gradle(&next_version)?;

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

fn next_version(repository: &Repository, last_version: &Version) -> Result<Version> {
    let commits = Commits::from_reference(
        repository,
        last_version.as_tag(),
        vec![],
        conventional_commits_next_version_lib::GitHistoryMode::FirstParent,
    )?;

    let version = commits.get_next_version(
        last_version.as_semver(),
        conventional_commits_next_version_lib::CalculationMode::Consecutive,
    );

    Ok(Version::from_semver(&version))
}

fn update_version_in_build_gradle(next_version: &Version) -> Result<()> {
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
