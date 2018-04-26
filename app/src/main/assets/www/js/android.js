document.addEventListener('DOMContentLoaded', function() {

    var n = Android.getDisplayName();
    var c = Android.getCopyRight();
    var h = Android.getHost();

    document.getElementById('name').innerHTML = n;
    document.getElementById('host').innerHTML = h;
    document.getElementById('right').innerHTML = " © " + c;

    if (c == "") document.getElementById('hr').style.display = "none";

    document.getElementById('reload').onclick = function() {
        window.location.href = Android.reloadPage();
    };
    document.getElementById('about').onclick = function() {
        Android.aboutApp();
    };
});