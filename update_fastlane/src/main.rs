use anyhow::{anyhow, Result};
use serde::Deserialize;
use std::{
    fs::{self, DirEntry, OpenOptions},
    io::Write,
    path::PathBuf,
};

#[derive(Deserialize)]
struct StoreInfo {
    #[serde(rename = "store_short_description")]
    pub short_description: Option<String>,
    #[serde(rename = "store_full_description")]
    pub full_description: Option<String>,
}

fn main() -> Result<()> {
    let files = fs::read_dir("../store-listing")?;
    let files = files
        .filter_map(Result::ok)
        .filter(|d| {
            if let Some(e) = d.path().extension() {
                e == "json"
            } else {
                false
            }
        })
        .collect::<Vec<_>>();

    for entry in files {
        update_fastlane_for_entry(entry)?;
    }

    Ok(())
}

fn update_fastlane_for_entry(entry: DirEntry) -> Result<()> {
    let filename = entry
        .file_name()
        .to_str()
        .ok_or(anyhow!("Couldn't get filename"))?
        .to_owned();

    let content = fs::read_to_string(entry.path())?;

    let store_info = serde_json::from_str::<StoreInfo>(&content)?;

    let mut dir_path = PathBuf::from("../fastlane/metadata/android");
    let dirname = filename.trim_end_matches(".json");
    dir_path.push(dirname);

    fs::create_dir_all(dir_path.clone())?;

    if let Some(short_description) = &store_info.short_description {
        let mut file_path = dir_path.clone();
        file_path.push("short_description.txt");

        let mut file = OpenOptions::new()
            .write(true)
            .create(true)
            .truncate(true)
            .open(file_path)?;

        file.write_all(&short_description.clone().into_bytes())?;
        file.write_all(b"\n")?;
    }

    if let Some(full_description) = &store_info.full_description {
        let mut file_path = dir_path.clone();
        file_path.push("full_description.txt");

        let mut file = OpenOptions::new()
            .write(true)
            .create(true)
            .truncate(true)
            .open(file_path)?;

        file.write_all(&full_description.clone().into_bytes())?;
        file.write_all(b"\n")?;
    }

    Ok(())
}
