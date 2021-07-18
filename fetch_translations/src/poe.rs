use serde::Deserialize;

#[derive(Deserialize, Debug)]
pub struct Translation {
    pub content: String,
}

#[derive(Deserialize, Debug)]
pub struct Term {
    pub term: String,
    pub translation: Translation,
    pub tags: Vec<String>,
}

#[derive(Deserialize, Debug)]
pub struct TermResult {
    pub terms: Vec<Term>,
}
#[derive(Deserialize, Debug)]
pub struct TermResponse {
    pub result: TermResult,
}
