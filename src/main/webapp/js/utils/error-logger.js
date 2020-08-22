'use strict';

window.onerror = function (message, file, line, col, error) {
    $.ajax({
        type: "POST",
        url: "/be/log-fe-error",
        data: JSON.stringify({error, message, file, line, col, appVersion: APP_VERSION}),
        contentType: "application/json; charset=utf-8"
    });
    return true;
};
