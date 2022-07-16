use std::fmt::Display;
use std::path::PathBuf;

use anyhow::anyhow;
use anyhow::Ok;
use anyhow::Result;
use git2::Repository;

const PROJECT_DIR: &str = env!("CARGO_MANIFEST_DIR");

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy)]
struct Version {
    major: u32,
    minor: u32,
    fix: u32,
}

impl Version {
    fn from_tag(tag: &str) -> Result<Self> {
        let parts: Vec<_> = tag[1..].split('.').collect();
        if parts.len() != 3 {
            return Err(anyhow!("Wrong tag"));
        }

        let major = parts[0].parse::<u32>()?;
        let minor = parts[1].parse()?;
        let fix = parts[2].parse()?;

        Ok(Self { major, minor, fix })
    }
}

impl Display for Version {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_fmt(format_args!(
            "v{major}.{minor}.{fix}",
            major = self.major,
            minor = self.minor,
            fix = self.fix
        ))
    }
}

fn main() -> Result<()> {
    let repository =
        Repository::open(PathBuf::from(PROJECT_DIR).join("..")).expect("Couldn't find git repo");

    let latest_version = latest_version(&repository)?;

    println!("Latest version: {latest_version}");

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
