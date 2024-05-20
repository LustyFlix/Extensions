// use an integer for version numbers
version = 1


streamverse {
    // All of these properties are optional, you can safely remove them

    description = "PornHub.com"
    authors = listOf("Queen Medusa")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1 // will be 3 if unspecified

    // List of video source types. Users are able to filter for extensions in a given category.
    // You can find a list of avaliable types here:
    // https://lustyflix.github.io/streamverse/html/app/com.lustyflix.streamverse/-tv-type/index.html
    tvTypes = listOf("NSFW")

    iconUrl = "https://www.google.com/s2/favicons?domain=pornhub.com&sz=%size%"

    language = "en"
}
