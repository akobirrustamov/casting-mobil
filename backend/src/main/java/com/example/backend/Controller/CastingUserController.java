package com.example.backend.Controller;

import com.example.backend.DTO.CastingUserDTO;
import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import com.example.backend.Entity.Message;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.Repository.MessageRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/casting-user")
@RequiredArgsConstructor
public class CastingUserController {
    private final CastingUserRepo castingUserRepo;
    private final AttachmentRepo attachmentRepo;
    private final MessageRepo messageRepo;
    @GetMapping
    public HttpEntity<?> getAllCastingUser(){
        List<CastingUser> all = castingUserRepo.findAllByOrderByCreatedAtDesc();
        return new ResponseEntity<>(all, HttpStatus.OK);
    }
    @GetMapping("/web")
    public HttpEntity<?> getAllCastingUserWeb(){
        List<CastingUser> all = castingUserRepo.findAllByIsWebShow(true);
        return new ResponseEntity<>(all, HttpStatus.OK);
    }


    @PostMapping
    public HttpEntity<?> addCastingUser(@RequestBody CastingUserDTO castingUser){
        System.out.println(castingUser);
        List<Attachment> attachments = new ArrayList<>();
        for (UUID photo : castingUser.getPhotos()) {
            Attachment attachment = new Attachment();
            Optional<Attachment> byId = attachmentRepo.findById(photo);
            if (byId.isPresent()) {
                attachment.setId(byId.get().getId());
                attachments.add(attachment);
            }

        }
        System.out.println(attachments);

        CastingUser castingUser1 = new CastingUser(castingUser.getTelegramId(), castingUser.getCastingType(), castingUser.getGender(), castingUser.getName(), castingUser.getRegion(), castingUser.getNationality(), castingUser.getBirthday(), castingUser.getAge(), castingUser.getHeight(), castingUser.getHairColor(), castingUser.getEyeColor(), castingUser.getClothSize(), castingUser.getShoeSize(), castingUser.getBust(), castingUser.getWaist(),castingUser.getSon(), castingUser.getEmail(), castingUser.getPhone(), castingUser.getTelegram(), castingUser.getFacebook(), castingUser.getInstagram(), castingUser.getPrice(), 0, attachments ,LocalDateTime.now());
        castingUser1.setIsWebShow(Boolean.FALSE);
        castingUser1.setFirstChan(0);
        castingUser1.setSecondChan(0);
        castingUserRepo.save(castingUser1);
        return new ResponseEntity<>(castingUser1, HttpStatus.CREATED);
    }
    @PutMapping("/web-show/{userId}")
    public HttpEntity<?> changeBoolean(@PathVariable Integer userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body("userId bo'sh bo'lishi mumkin emas");
        }

        Optional<CastingUser> byId = castingUserRepo.findById(userId);
        if (byId.isPresent()) {
            CastingUser castingUser = byId.get();

            Boolean current = castingUser.getIsWebShow();
            if (current == null) {
                current = false; // default qiymat
            }

            castingUser.setIsWebShow(!current);
            castingUserRepo.save(castingUser);

            return ResponseEntity.ok(castingUser);
        }
        return ResponseEntity.notFound().build();
    }



    @GetMapping("/payed/{castingUserId}")
    public HttpEntity<?> getPayedCastingUser(@PathVariable Integer castingUserId){
        Optional<CastingUser> byId = castingUserRepo.findById(castingUserId);
        if (byId.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        CastingUser castingUser = byId.get();
        castingUser.setSecondChan(1);
        castingUserRepo.save(castingUser);
        return new ResponseEntity<>(castingUser, HttpStatus.OK);
    }


    @PutMapping("/status/{castingUserId}/{status}/{price}")
    public HttpEntity<?> updateCastingUserStatus(@PathVariable Integer castingUserId, @PathVariable Integer status, @PathVariable String price){
        Optional<CastingUser> byId = castingUserRepo.findById(castingUserId);
        if (byId.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        byId.get().setStatus(status);
        CastingUser save = castingUserRepo.save(byId.get());

//        status=0 new
//        status=1 qabul qilindi
//        status=2 Rad etildi


        Map<String, String> castingTypeTranslation = Map.of(
                "actor", "Aktyor",
                "extra", "Aktrisa",
                "model", "Model",
                "euromodel", "Yevro model",
                "bloger", "Bloger",
                "influencer", "Reklama"
        );

        String translatedType = castingTypeTranslation.getOrDefault(save.getCastingType(), save.getCastingType());

        Optional<Message> message = messageRepo.findByCastingUserId(castingUserId);
        if (message.isEmpty()){
            if (status == 1){

                Message message1 = new Message(save,"\uD83D\uDFE2Siz Castingdan o'tdingiz!", price,  LocalDateTime.now(), true );
                message1.setTelegramId(new BigInteger(save.getTelegramId()));
                message1.setName(save.getName());
                message1.setCastingType(translatedType);
                messageRepo.save(message1);
            }
            if (status == 2){
                Message message2 = new Message(save,"⛔\uFE0F Afsuski, so‘rovingiz rad etildi! Castingdan o'ta olmadingiz.", "0",  LocalDateTime.now(), false);
                message2.setTelegramId(new BigInteger(save.getTelegramId()));
                message2.setName(save.getName());
                message2.setCastingType(translatedType);
                messageRepo.save(message2);
            }
        }else{
            Message message3 = message.get();
            message3.setStatus(status==1);
            message3.setPrice(price);
            message3.setName(save.getName());
            message3.setCastingType(translatedType);
            messageRepo.save(message3);
        }





        return new ResponseEntity<>(HttpStatus.OK);

    }



    @PutMapping("/price/{castingUserId}/{price}")
    public HttpEntity<?> updateCastingUserStatus(@PathVariable Integer castingUserId, @PathVariable Double price){
        Optional<CastingUser> byId = castingUserRepo.findById(castingUserId);
        if (byId.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        byId.get().setPrice(price);
        castingUserRepo.save(byId.get());
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @DeleteMapping("/{castingUserId}")
    public HttpEntity<?> deleteCastingUser(@PathVariable Integer castingUserId){
        messageRepo.deleteByCastinguserId(castingUserId);
       castingUserRepo.deleteById(castingUserId);
       return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/my/{telegramId}")
    public HttpEntity<?> getMyCastingUser(@PathVariable String telegramId){
       List<CastingUser> castingUsers = castingUserRepo.findByTelegramId(telegramId);
       return ResponseEntity.ok(castingUsers);
    }

    @GetMapping("/appeal/{appealId}")
    public HttpEntity<?> getAppealUser(@PathVariable Integer appealId){
        System.out.println(appealId);
        Optional<CastingUser> byId = castingUserRepo.findById(appealId);
        if (byId.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        System.out.println(byId.get());
        return ResponseEntity.ok(byId.get());
    }








}
