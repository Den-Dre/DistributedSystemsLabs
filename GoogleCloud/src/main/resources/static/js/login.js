let firebaseConfig;
if (location.hostname === "localhost") {
    console.log("Localhost")
    firebaseConfig = {
        apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
        projectId: "demo-distributed-systems-kul",
    }
} else {
    console.log("Remote!!")
    // TODO: (level 2) replace with your own configuration
    firebaseConfig = {
        apiKey: "AIzaSyBbfO9SfQbkTaJB5pg-74DPrBkt6nIApgE",
        authDomain: "distributedsystemspart2.firebaseapp.com",
        projectId: "distributedsystemspart2",
        storageBucket: "distributedsystemspart2.appspot.com",
        messagingSenderId: "1001499911733",
        appId: "1:1001499911733:web:2d16ee1f661299211d337c"
    }
}
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
if (location.hostname === "localhost") {
    auth.useEmulator("http://localhost:8082");
}
const ui = new firebaseui.auth.AuthUI(auth);

ui.start('#firebaseui-auth-container', {
    signInOptions: [
        firebase.auth.EmailAuthProvider.PROVIDER_ID
    ],
    callbacks: {
        signInSuccessWithAuthResult: function (authResult, redirectUrl) {
            auth.currentUser.getIdToken(true)
                .then(async (idToken) => {
                    await fetch("/authenticate", {
                        method: "POST",
                        body: idToken,
                        headers: {
                            "Content-Type": "plain/text"
                        }
                    });
                    location.assign("/");
                });
            return false;
        },
    },
});
