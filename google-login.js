(function () {
  var btn = document.querySelector("[data-google-popup]");
  if (!btn || !window.GOOGLE_CLIENT_ID) {
    return;
  }

  function postCredential(jwt) {
    var form = document.createElement("form");
    form.method = "POST";
    form.action = "/auth/google/id-token";
    var cred = document.createElement("input");
    cred.type = "hidden";
    cred.name = "credential";
    cred.value = jwt;
    var fromInput = document.createElement("input");
    fromInput.type = "hidden";
    fromInput.name = "from";
    fromInput.value = btn.getAttribute("data-google-from") || "login";
    form.appendChild(cred);
    form.appendChild(fromInput);
    document.body.appendChild(form);
    form.submit();
  }

  function attach() {
    if (!window.google || !google.accounts || !google.accounts.id) {
      return false;
    }
    google.accounts.id.initialize({
      client_id: window.GOOGLE_CLIENT_ID,
      callback: function (resp) {
        if (resp && resp.credential) {
          postCredential(resp.credential);
        }
      },
      ux_mode: "popup",
      auto_select: false,
      context: "signin",
      itp_support: true
    });
    btn.addEventListener("click", function (event) {
      event.preventDefault();
      google.accounts.id.prompt(function (notification) {
        if (notification && (notification.isNotDisplayed() || notification.isSkippedMoment() || notification.isDismissedMoment())) {
          var client = google.accounts.oauth2 && google.accounts.oauth2.initCodeClient
            ? google.accounts.oauth2.initCodeClient({
                client_id: window.GOOGLE_CLIENT_ID,
                scope: "openid email profile",
                ux_mode: "popup",
                callback: function (resp) {
                  if (!resp || !resp.code) {
                    window.location.href = btn.getAttribute("href");
                    return;
                  }
                  var form = document.createElement("form");
                  form.method = "POST";
                  form.action = "/auth/google/popup";
                  var codeInput = document.createElement("input");
                  codeInput.type = "hidden";
                  codeInput.name = "code";
                  codeInput.value = resp.code;
                  var fromInput = document.createElement("input");
                  fromInput.type = "hidden";
                  fromInput.name = "from";
                  fromInput.value = btn.getAttribute("data-google-from") || "login";
                  form.appendChild(codeInput);
                  form.appendChild(fromInput);
                  document.body.appendChild(form);
                  form.submit();
                }
              })
            : null;
          if (client) {
            client.requestCode();
          } else {
            window.location.href = btn.getAttribute("href");
          }
        }
      });
    });
    return true;
  }

  if (attach()) {
    return;
  }
  var tries = 0;
  var timer = setInterval(function () {
    tries += 1;
    if (attach() || tries > 40) {
      clearInterval(timer);
    }
  }, 250);
})();
