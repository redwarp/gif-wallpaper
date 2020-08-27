// This updater is used by standard-version to update our GlobalDependency file.
const versionNameRegex = /(versionName ")([0-9]+\.[0-9]+\.[0-9]+)(")/g
const versionCodeRegex = /(versionCode )([0-9]+)/g

module.exports.readVersion = function (contents) {
    return versionNameRegex.exec(contents)[2]
}

module.exports.writeVersion = function (contents, version) {
    var updatedContent = contents.replace(versionNameRegex, "$1" + version + "$3")
    var versionCode = parseInt(versionCodeRegex.exec(contents)[2]) + 1
    return updatedContent.replace(versionCodeRegex, "$1" + versionCode)
}