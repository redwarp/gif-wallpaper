mod poe;
mod xml;

use anyhow::Context;
use anyhow::Error;
use anyhow::Result;
use poe::TermResponse;
use reqwest::Client;
use serde::Deserialize;
use xml::Language as SupportedLanguage;
use xml::Strings;

#[derive(Deserialize, Debug)]
struct Language {
    name: String,
    code: String,
    percentage: f32,
}
#[derive(Deserialize, Debug)]
struct LanguageResult {
    languages: Vec<Language>,
}
#[derive(Deserialize, Debug)]
struct LanguageResponse {
    result: LanguageResult,
}

#[tokio::main]
async fn main() -> Result<()> {
    let client = reqwest::Client::new();

    let content = fetch_languages(&client).await?;

    println!("Printing fetched content: {:?}", content);

    for language in &content {
        let supported_language = SupportedLanguage::from(&language.code).ok_or(Error::msg(
            format!("Unsupported language {}", language.code),
        ))?;

        let terms = fetch_terms(&client, &language.code).await?;

        let strings = Strings::from(supported_language, terms);

        println!("{:?}", strings)
    }

    Ok(())
}

async fn fetch_languages(client: &Client) -> Result<Vec<Language>> {
    let response = client
        .post("https://api.poeditor.com/v2/languages/list")
        .form(&[
            ("api_token", "259aa66dfbdccf0ed87bf09855e3a861"),
            ("id", "435211"),
        ])
        .send()
        .await?
        .json::<LanguageResponse>()
        .await?;

    Ok(response.result.languages)
}

async fn fetch_terms(client: &Client, language_code: &String) -> Result<TermResponse> {
    // Must use untagged for the plural stuff: https://github.com/serde-rs/json/issues/473
    // as the content can be either string, or plural.

    let response = client
        .post("https://api.poeditor.com/v2/terms/list")
        .form(&[
            ("api_token", "259aa66dfbdccf0ed87bf09855e3a861"),
            ("id", "435211"),
            ("language", language_code.as_str()),
        ])
        .send()
        .await?
        .json::<TermResponse>()
        .await?;

    Ok(response)
}
