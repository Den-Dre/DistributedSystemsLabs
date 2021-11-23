package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PubSubController {
    private final Model model;

    @Autowired
    public PubSubController(Model model) {
        this.model = model;
    }

    // https://cloud.google.com/pubsub/docs/troubleshooting#messages
    // <- Google documentatie is officieel poep
    @PostMapping("_ah/push-handlers/test")
    public ResponseEntity<Void> confirmQuote(@RequestBody String message) throws ParseException {
        System.out.println("Got into endpoint");
        System.out.println(message);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
