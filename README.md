# Secure Chat Application with Signal Protocol


In this app, we covered a chat application with using Signal protocol. 

## Summary of Application
In this application, E2E Encryption is targeted by keeping the messages in the server (firebase realtime database) as ciphertext. We used Signal encryption to accomplish this.

## Entity
It is a class in which keys are kept and stored in the entity object in our application. It contains SignalProtocolStore and SignalProtocolAddress objects that must be kept secret. In other words, these objects are not shared with the server, they are created every time the user logs in. 
```
    private final SignalProtocolStore store;
    private final PreKeyBundle preKey;
    private final SignalProtocolAddress address;
```
For this, we created the StoreMaker and KeyPairsMaker classes. In addition, the Entity class contains the PreKeyBundle object. This PreKeyBundle is shared with the server for user authentication. It is a public information.
## Session
Session class is a tool that provides communication between 2 Entities. It includes Encrypt and Decrypt method. The Encrypt method receives a String variable (message entered by the user), encrypts it and returns it in PreKeySignalMessage. The returned PreKeySignalMessage is encoded and kept on the server. In order to decode this encrypted message, it is decoded from the server and the Decrypt method is applied. 

```
public PreKeySignalMessage encrypt(String message) {
            .
            .
            .
            PreKeySignalMessage encrypted = new PreKeySignalMessage(rawCiphertext);
            return encrypted;
    
    }
 public String decrypt(PreKeySignalMessage ciphertext) {
            .
            .
            .
            byte[] decrypted = cipher.decrypt(ciphertext);
            return new String(decrypted, UTF8);
     
    }
```
While decrypt, it should be remembered to store passwords for KDF Chain.
```
signalProtocolStore.storePreKey(alicePreKeyBundle.getPreKeyId(),preKeyRecord);
signalProtocolStore.storeSignedPreKey(alicePreKeyBundle.getSignedPreKeyId(),signedPreKeyRecord);
```
## Key Store
There are 2 types of key store methods in practice. The first method applied to keys that should be kept secret is to store in the local database (sqlite). These keys are generated while the user is registered, and are thrown into the local database. When the user logs in again, keys are created by decoding from the database.
```
database.execSQL("INSERT INTO SignalPrivates (id,storeMaker, keyPairMaker) VALUES ('"+id+"','"+stringStoreMaker+"','"+Entity.keyPairMakerString+"')"); 
```

The second method is the keys that must be public to verify the user's credentials. This collection of keys is called PreKeyBundle. This bundle is shared on the firebase server.

```
 PreKeyBundleMaker preKeyBundleMaker=PreKeyBundleMaker.builder()
                                    .registrationId(registrationId)
                                    .deviceId(deviceId)
                                    .preKeyId(preKeyId)
                                    .preKeyPublic(preKeyPublic)
                                    .signedPreKeyId(signedPreKeyId)
                                    .signedPreKeyPublic(signedPreKeyPublic)
                                    .identityPreKeySignature(identityPreKeySignature)
                                    .identityKey(identityKey)
                                    .build();
.
.
.
FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(user); //We set this preKeeyBundleMaker object into user object. And shared user object to the server.

```
<p align="left">
<img   src="https://github.com/zahitkaya/chat-app/blob/master/images/publicKeys.PNG"  width="70%" height="70%"/> 
</p>

## Message Store

In order to ensure security, encrypted versions of messages should be kept on the server. Messages are kept on the server until they are decrypt.
```
Message message=Message.builder()
         .message(signalCipherText)
         .receiver(receiverEmail)
         .sender(mAuth.getCurrentUser().getEmail())
         .msgTimeStamp(timestamp)
         .decrypted(false)
         .build();
         
FirebaseDatabase.getInstance().getReference("Messages").child(sortedUid).child(timestamp).setValue(message);

```

<p align="left">
<img  src="https://github.com/zahitkaya/chat-app/blob/master/images/encryptedMessages.PNG" width="70%" height="70%" >
</p>

<p align="center">
<img src="https://github.com/zahitkaya/chat-app/blob/master/images/Screenshot_1616186234.png" width="25%" height="25%" >
</p>

## Used Technologies
* Firebase Database 19.6
* Firebase Auth 20.0.3
* Toasty 1.5.0
* Lombok 1.18.16
* Java Cryptography Architecture
