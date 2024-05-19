// use an integer for version numbers
version = 1


streamverse {
    language = "en"
    // All of these properties are optional, you can safely remove them

     description = "PornWatch.Ws"
     authors = listOf("Queen Medusa")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Movie",
    )

    iconUrl = "https://pornwatch.ws/wp-content/uploads/2023/04/favicon-4.png"
}
