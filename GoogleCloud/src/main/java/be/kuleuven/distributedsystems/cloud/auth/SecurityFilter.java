package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private final WebClient.Builder builder = WebClient.builder();

    @Resource(name = "isProduction")
    private boolean isProduction;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var session = WebUtils.getCookie(request, "session");
        User user;
        DecodedJWT authorizedJWT;
        if (session != null) {
            try {
                // 1. Decode the JWT
                DecodedJWT idToken = JWT.decode(session.getValue());

                // Try to verify the jwt if we are in production
                if (isProduction) {
                    // 2. Get the Key Id from the decoded jwt
                    var kid = idToken.getKeyId();

                    // 3. Fetch the google public keys
                    JsonObject googleKeys = getGoogleKeys();
                    var pubkey = googleKeys.get(kid).getAsString();
                    // Trying to do the parsing as done here:
                    // https://stackoverflow.com/questions/9739121/convert-a-pem-formatted-string-to-a-java-security-cert-x509certificate
                    X509Certificate cert = convertStringToX509Cert(pubkey);
                    RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

                    // 4. Verify the decoded jwt
                    Algorithm algorithm = Algorithm.RSA256(publicKey, null);
                    authorizedJWT = JWT.require(algorithm)
                            .withIssuer("https://securetoken.google.com/" + "distributedsystemspart2")
                            .build()
                            .verify(idToken); // This verifies all the claims as mentioned on:
                    // https://firebase.google.com/docs/auth/admin/verify-id-tokens#verify_id_tokens_using_a_third-party_jwt_library
                } else {
                    authorizedJWT = idToken;
                }

                // 5. Use the claims from the verified jwt
                String role = authorizedJWT.getClaim("role").asString();
                if (!"manager".equals(role))
                    role = "";

                String mail = authorizedJWT.getClaim("email").asString();
                user = new User(mail, role);

                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(new FirebaseAuthentication(user));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // TODO: (level 1) decode Identity Token and assign correct email and role (Done? YYAAS)
            // TODO: (level 2) verify Identity Token
        }
        filterChain.doFilter(request, response);
    }

    private X509Certificate convertStringToX509Cert(String certificate) throws Exception{
        InputStream targetStream = new ByteArrayInputStream(certificate.getBytes());
        return (X509Certificate) CertificateFactory
                .getInstance("X509")
                .generateCertificate(targetStream);
    }

    private JsonObject getGoogleKeys() {
        URL url;
        HttpURLConnection con = null;
        try {
            url = new URL("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
//            // replace with builder
//            builder
//                    .baseUrl("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
//                    .build()
//                    .get()
//                    .retrieve()
//                    //.bodyToMono(new ParameterizedTypeReference<Ticket>() {})
//                    .
//                    .block();

            JsonParser parser = new JsonParser();
            JsonObject obj  = parser.parse(content.toString()).getAsJsonObject();

//            System.out.println("GET request contents: " + content);
//            System.out.println("GET request status: " +  status);
            in.close();
            return obj;
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            if (con != null)
                con.disconnect();
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/authenticate") || path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css");
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.isManager()) {
                return List.of(new SimpleGrantedAuthority("manager"));
            } else{
                return new ArrayList<>();
            }
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {


        }

        @Override
        public String getName() {
            return null;
        }
    }
}

