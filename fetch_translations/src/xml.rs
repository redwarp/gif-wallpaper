use crate::poe::TermResponse;

#[derive(Debug)]
pub struct Strings {
    pub language: Language,
    pub terms: Vec<Term>,
}

impl Strings {
    pub fn from(language: Language, response: TermResponse) -> Self {
        let app = "app".to_string();
        let res: Vec<Term> = response
            .result
            .terms
            .into_iter()
            .filter(|term| term.tags.contains(&app))
            .map(|term| Term {
                key: term.term,
                value: term.translation.content,
            })
            .collect();

        Strings {
            language,
            terms: res,
        }
    }
}

#[derive(Debug)]
pub struct Term {
    key: String,
    value: String,
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

    fn values_folder(self) -> String {
        match self {
            Self::English => ("values".to_string()),
            Self::French => ("values-fr".to_string()),
            Self::German => ("values-de".to_string()),
            Self::Russian => ("values-ru".to_string()),
            Self::Spanish => ("values-es".to_string()),
        }
    }
}
