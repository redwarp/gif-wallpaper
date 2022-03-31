use anyhow::Result;
use serde::Deserialize;
use std::{
    fs::{self, DirEntry, OpenOptions},
    io::Write,
    path::{Path, PathBuf},
};

const PROJECT_DIR: &str = env!("CARGO_MANIFEST_DIR");

#[derive(Deserialize)]
struct StoreInfo {
    #[serde(rename = "app_name")]
    pub title: Option<String>,
    #[serde(rename = "store_full_description")]
    pub full_description: Option<String>,
    #[serde(rename = "store_short_description")]
    pub short_description: Option<String>,
}

fn main() -> Result<()> {
    let files = fs::read_dir(PathBuf::from(PROJECT_DIR).join("../store-listing"))?;
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
    let filename = entry.file_name();
    let filename = filename.to_string_lossy();

    let content = fs::read_to_string(entry.path())?;

    let store_info = serde_json::from_str::<StoreInfo>(&content)?;

    let dirname = filename.trim_end_matches(".json");
    let dir_path = PathBuf::from(PROJECT_DIR)
        .join("../fastlane/metadata/android")
        .join(dirname);

    fs::create_dir_all(&dir_path)?;

    if let Some(title) = &store_info.title {
        write_file(&dir_path, "title.txt", title)?;
    }

    if let Some(full_description) = &store_info.full_description {
        write_file(&dir_path, "full_description.txt", full_description)?;
    }

    if let Some(short_description) = &store_info.short_description {
        write_file(&dir_path, "short_description.txt", short_description)?;
    }

    Ok(())
}

fn write_file(directory: &Path, filename: &str, content: &str) -> Result<()> {
    let file_path = directory.join(filename);
    let mut file = OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(file_path)?;

    file.write_all(content.as_bytes())?;
    file.write_all(b"\n")?;

    Ok(())
}
