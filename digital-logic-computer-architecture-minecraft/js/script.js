function updateToc() {
    targetUrl = (document.getElementById("bookViewport").contentWindow.location.href)

    start = targetUrl.indexOf('/book/') + 6
    end = targetUrl.indexOf('.html')

    // target unit and chapter to highlight
    targetChapter = targetUrl.slice(start,end)


    // highlight appropriate ID
    $(".toc").removeClass("here");

    // add the class to the current TOC entry that we're at
    $("#" + targetChapter).addClass("here")


}