use crate::poe::TermResponse;
use anyhow::Result;
use convert_case::{Case, Casing};
use serde_json::json;
use std::{
    collections::HashMap,
    fs::{self, File},
    path::PathBuf,
};
use xml::writer::{EmitterConfig, XmlEvent};

#[derive(Debug)]
pub struct Strings {
    pub language: Language,
    pub terms: Vec<Term>,
}

#[derive(Debug, PartialEq)]
pub enum StringType {
    APP,
    STORE,
    OTHER,
}

impl Strings {
    pub fn from(language: Language, response: TermResponse) -> Self {
        let mut res: Vec<Term> = response
            .result
            .terms
            .into_iter()
            .map(|term| {
                let string_types: Vec<_> = term
                    .tags
                    .iter()
                    .map(|tag| match tag.as_str() {
                        "app" => StringType::APP,
                        "store" => StringType::STORE,
                        _ => StringType::OTHER,
                    })
                    .collect();
                Term {
                    key: term.term,
                    value: term.translation.content,
                    string_types: string_types,
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
        let file_path = PathBuf::from("../store-listing").join(self.language.store_listing_file());

        let map: HashMap<_, _> = self
            .terms
            .iter()
            .filter(|term| term.string_types.contains(&StringType::STORE))
            .map(|term| (term.key.clone(), term.value.clone()))
            .collect();

        let json = json!(map);
        fs::write(file_path, serde_json::to_string_pretty(&json)?)?;

        Ok(())
    }

    pub fn write_xml(&self) -> Result<()> {
        let file_path = PathBuf::from("../app/src/main/res")
            .join(self.language.values_folder())
            .join("strings.xml");

        let mut file = File::create(file_path)?;
        let mut writer = EmitterConfig::new()
            .perform_indent(true)
            .create_writer(&mut file);

        writer.write(XmlEvent::start_element("resources"))?;
        for term in self
            .terms
            .iter()
            .filter(|term| term.string_types.contains(&StringType::APP) && term.value.len() > 0)
        {
            let key = term.key.to_case(Case::Snake);
            let value = format!("\"{}\"", term.value.replace("\"", "\\\""));
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

#[derive(Debug)]
pub enum Language {
    English,
    French,
    German,
    Russian,
    Spanish,
}

impl Language {
    pub fn from<T>(language_code: &T) -> Option<Language>
    where
        T: Into<String> + Clone,
    {
        match &language_code.to_owned().into()[..] {
            "en" => Some(Self::English),
            "fr" => Some(Self::French),
            "de" => Some(Self::German),
            "ru" => Some(Self::Russian),
            "es" => Some(Self::Spanish),
            _ => None,
        }
    }

    fn values_folder(&self) -> String {
        match self {
            Self::English => ("values".to_string()),
            Self::French => ("values-fr".to_string()),
            Self::German => ("values-de".to_string()),
            Self::Russian => ("values-ru".to_string()),
            Self::Spanish => ("values-es".to_string()),
        }
    }

    fn store_listing_file(&self) -> String {
        match self {
            Self::English => ("en-US.json".to_string()),
            Self::French => ("fr-FR.json".to_string()),
            Self::German => ("de-DE.json".to_string()),
            Self::Russian => ("ru-RU.json".to_string()),
            Self::Spanish => ("es-ES.json".to_string()),
        }
    }
}
