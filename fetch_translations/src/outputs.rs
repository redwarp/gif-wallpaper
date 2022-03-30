use anyhow::Result;
use convert_case::{Case, Casing};
use serde_json::json;
use std::{
    collections::HashMap,
    fs::{self, create_dir_all, File},
    path::PathBuf,
};
use xml::writer::{EmitterConfig, XmlEvent};

use crate::{configuration::Language, poe::TermResponse};

const PROJECT_DIR: &str = env!("CARGO_MANIFEST_DIR");

#[derive(Debug)]
pub struct Strings<'a> {
    pub language: &'a Language,
    pub terms: Vec<Term>,
}

#[derive(Debug, PartialEq)]
pub enum StringType {
    App,
    Store,
    Other,
}

impl<'a> Strings<'a> {
    pub fn from(language: &'a Language, response: TermResponse) -> Self {
        let mut res: Vec<Term> = response
            .result
            .terms
            .into_iter()
            .map(|term| {
                let string_types: Vec<_> = term
                    .tags
                    .iter()
                    .map(|tag| match tag.as_str() {
                        "app" => StringType::App,
                        "store" => StringType::Store,
                        _ => StringType::Other,
                    })
                    .collect();
                Term {
                    key: term.term,
                    value: term.translation.content.trim().to_string(),
                    string_types,
                }
            })
            .collect();
        res.sort_by(|a, b| a.key.cmp(&b.key));

        Strings {
            language,
            terms: res,
        }
    }

    pub fn write_json(&self) -> Result<()> {
        let file_path = PathBuf::from(PROJECT_DIR)
            .join("../store-listing")
            .join(&self.language.store_listing_file);

        let map: HashMap<_, _> = self
            .terms
            .iter()
            .filter(|term| term.string_types.contains(&StringType::Store))
            .map(|term| (term.key.clone(), term.value.clone()))
            .collect();

        let json = json!(map);
        fs::write(file_path, serde_json::to_string_pretty(&json)?)?;

        Ok(())
    }

    pub fn write_xml(&self) -> Result<()> {
        let folder = PathBuf::from(PROJECT_DIR)
            .join("../app/src/main/res")
            .join(&self.language.values_folder);
        create_dir_all(&folder)?;
        let file_path = folder.join("strings.xml");

        let mut file = File::create(file_path)?;
        let mut writer = EmitterConfig::new()
            .perform_indent(true)
            .create_writer(&mut file);

        writer.write(XmlEvent::start_element("resources"))?;
        for term in self
            .terms
            .iter()
            .filter(|term| term.string_types.contains(&StringType::App) && !term.value.is_empty())
        {
            let key = term.key.to_case(Case::Snake);
            let value = format!("\"{}\"", term.value.replace('"', "\\\""));
            let start = XmlEvent::start_element("string").attr("name", key.as_str());
            writer.write(start)?;
            writer.write(XmlEvent::characters(value.as_str()))?;
            writer.write(XmlEvent::end_element())?;
        }
        writer.write(XmlEvent::end_element())?;

        Ok(())
    }
}

#[derive(Debug)]
pub struct Term {
    key: String,
    value: String,
    string_types: Vec<StringType>,
}
