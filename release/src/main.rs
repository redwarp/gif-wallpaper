use std::fmt::Display;
use std::path::PathBuf;

use anyhow::anyhow;
use anyhow::Ok;
use anyhow::Result;
use conventional_commits_next_version_lib::Commits;
use git2::Repository;

const PROJECT_DIR: &str = env!("CARGO_MANIFEST_DIR");

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
        format!("{self}")
    }

    fn as_string(&self) -> String {
        format!(
            "{major}.{minor}.{patch}",
            major = self.major,
            minor = self.minor,
            patch = self.patch
        )
    }

    fn as_semver(&self) -> semver::Version {
        semver::Version::new(self.major, self.minor, self.patch)
    }
}

impl Display for Version {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_fmt(format_args!(
            "v{major}.{minor}.{patch}",
            major = self.major,
            minor = self.minor,
            patch = self.patch
        ))
    }
}

fn main() -> Result<()> {
    let repository =
        Repository::open(PathBuf::from(PROJECT_DIR).join("..")).expect("Couldn't find git repo");

    let last_version = latest_version(&repository)?;

    println!("Latest version: {last_version}");

    let next_version = next_version(&repository, &last_version);

    println!("Next version: {next_version:?}");

    Ok(())
}

fn latest_version(repository: &Repository) -> Result<Version> {
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

fn next_version(repository: &Repository, last_version: &Version) -> Result<semver::Version> {
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

    Ok(version)
}
