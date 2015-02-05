// assign click event to marker tabs
$("a[id^='tab_']").click(function () {
	// make sure the quickjump links for the corresponding marker tab are shown
	$("li[id^='quickjump_']").removeClass("show").addClass("hide");
	var tabId = this.id.substring(4); //substring after 'tab_'
	$("#quickjump_" + tabId).removeClass("hide").addClass("show");
});

