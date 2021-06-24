package com.company.controller;

import com.company.controller.database.MongoController;
import com.company.controller.hashers.HashAlgorithm;
import com.company.controller.hashers.SHA256Hasher;
import com.company.controller.tokens.TokenClaims;
import com.company.controller.tokens.TokenManager;
import com.company.exception.NotFoundException;
import com.company.exception.UnauthorizedException;
import com.company.model.DeleteChatroom.DeleteChatroomRequestBody;
import com.company.model.DeleteChatroom.DeleteChatroomResponse;
import com.company.model.GetChatrooms.GetChatroomsResponse;
import com.company.model.Login.LoginRequestBody;
import com.company.model.Login.LoginResponse;
import com.company.model.PostChatroom.PostChatroomRequestBody;
import com.company.model.PostChatroom.PostChatroomResponse;
import com.company.model.Register.RegisterRequestBody;
import com.company.model.Register.RegisterResponse;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
public class AuthController {

    private final HashAlgorithm hasher = new SHA256Hasher();

    @RequestMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequestBody body) {

        String hashedPassword = hasher.saltAndHash(body.getPassword());

        MongoController mc = new MongoController();
        mc.addUser(body.getUsername(), hashedPassword);

        Document result = mc.getUserWithUsername(body.getUsername());

        String insertedID = "";
        if (result != null) {
            insertedID = ((ObjectId) result.get("_id")).toString();
        }

        return new RegisterResponse(insertedID);

    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequestBody body) {

        MongoController mc = new MongoController();
        Document result = mc.getUserWithUsername(body.getUsername());
        if (result == null) {
            throw new UnauthorizedException();
        }

        String referencePassword = (String) result.get("password");
        boolean isPasswordValid = hasher.checkPassword(referencePassword, body.getPassword());
        if (!isPasswordValid) {
            throw new UnauthorizedException();
        }

        TokenManager tm = new TokenManager();
        String token = tm.generateToken(new TokenClaims(body.getUsername()));
        return new LoginResponse(token);
    }

    @GetMapping("/chatrooms")
    public GetChatroomsResponse getChatrooms(@RequestHeader(name = "Authorization") String authHeader) {

        TokenManager tm = new TokenManager();
        boolean claims = tm.verifyToken(authHeader);

        if (!claims) {
            throw new UnauthorizedException();
        }

        MongoController mc = new MongoController();

        ArrayList<Document> chatrooms = mc.getChatrooms();
        ArrayList<String> chatroomNames = new ArrayList<>();

        for (Document chatroom : chatrooms) {
            chatroomNames.add(chatroom.get("name").toString());
        }
        return new GetChatroomsResponse(chatroomNames);
    }

    @PostMapping("/chatrooms")
    public PostChatroomResponse addChatroom(@RequestHeader(name = "Authorization") String authHeader,
                                            @RequestBody PostChatroomRequestBody body) {

        TokenManager tm = new TokenManager();
        boolean claims = tm.verifyToken(authHeader);

        if (!claims) {
            throw new UnauthorizedException();
        }

        MongoController mc = new MongoController();
        mc.addChatroom(body.getName());

        Document result = mc.getChatRoomWithName(body.getName());

        String insertedID = "";
        if (result != null) {
            insertedID = ((ObjectId) result.get("_id")).toString();
        }

        return new PostChatroomResponse(insertedID);
    }

    @DeleteMapping("/chatrooms")
    public DeleteChatroomResponse deleteChatrooms(@RequestHeader(name = "Authorization") String authHeader,
                                                  @RequestBody DeleteChatroomRequestBody body) {

        TokenManager tm = new TokenManager();
        boolean claims = tm.verifyToken(authHeader);

        if (!claims) {
            throw new UnauthorizedException();
        }

        MongoController mc = new MongoController();

        Document result = mc.getChatRoomWithName(body.getName());

        if (result == null) {
            throw new NotFoundException();
        }

        mc.deleteChatroom(body.getName());

        return new DeleteChatroomResponse(body.getName() + " deleted");
    }


}
