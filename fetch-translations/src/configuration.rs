use std::{fs::File, path::Path};

use anyhow::Result;
use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct Language {
    pub code: String,
    pub values_folder: String,
    pub store_listing_file: String,
}

#[derive(Debug)]
pub struct Configuration {
    supported_languages: Vec<Language>,
}

impl Configuration {
    pub fn load_configuration() -> Result<Configuration> {
        let config_file = Path::new(env!("CARGO_MANIFEST_DIR")).join("config.json");

        let supported_languages: Vec<Language> = serde_json::from_reader(File::open(config_file)?)?;

        Ok(Configuration {
            supported_languages,
        })
    }

    pub fn language_by_code(&self, code: &str) -> Option<&Language> {
        self.supported_languages
            .iter()
            .find(|language| language.code == code)
    }
}
