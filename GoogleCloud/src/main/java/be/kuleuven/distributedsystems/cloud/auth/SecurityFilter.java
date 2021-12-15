package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minidev.json.JSONObject;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.eclipse.jetty.websocket.jsr356.annotations.JsrParamIdText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.interfaces.RSAKey;
import java.util.*;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private final WebClient.Builder builder = WebClient.builder();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var session = WebUtils.getCookie(request, "session");
        User user;
        if (session != null) {
            try {
                JsonObject googleKeys = getKeys();
                DecodedJWT jwt= JWT.decode(session.getValue());
                // System.out.println("JWT: " + jwt.getHeader());
                var kid = jwt.getKeyId();
                var signature = jwt.getSignature();
                System.out.println("kid: " + kid);

                var pubkey = googleKeys.get(kid).getAsString();
//                Algorithm algorithm = Algorithm.RSA256(pubkey, null);
//                DecodedJWT jwt1 = JWT.require(algorithm)
//                                .withIssuer("https://securetoken.google.com/" + "distributedsystemspart2")
//                                .build()
//                                .verify(signature);

                System.out.println("public key: " + pubkey);
                System.out.println("signature: " + signature);

                String role = jwt.getClaim("role").asString();
                if (!"manager".equals(role))
                        role = "";

                String mail = jwt.getClaim("email").asString();
                user = new User(mail, role);

            } catch (JWTDecodeException e) {
//                System.out.println(e);
//                System.out.println("decode-exception");
                user = new User("", "");
            }


            // TODO: (level 1) decode Identity Token and assign correct email and role (Done? YYAAS)
            // TODO: (level 2) verify Identity Token

            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(new FirebaseAuthentication(user));
        }
        filterChain.doFilter(request, response);
    }

    private JsonObject getKeys() {
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

