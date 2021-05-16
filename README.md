# Secure Chat Application with RSA 


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
## Generating Public and Private Keys
To provide RSA, all users must have a unique public and private key. For this, while the user is registered, we produce private public and private keys for the user.
And while producing them, we pair the keys (public and private key for a user) with each other. 
```
RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
        publicKey=Base64.getEncoder().encodeToString(keyPairGenerator.getPublicKey().getEncoded());
        privateKey=Base64.getEncoder().encodeToString(keyPairGenerator.getPrivateKey().getEncoded());
```
## Key Storage
The application keeps all public keys in database,

<p align="left">
<img   src="https://github.com/zahitkaya/chat-app/blob/master/images/publicKeys.PNG"  width="70%" height="70%"/> 
</p>

And it keeps private keys in storage using shared preference. 
```
        SharedPreferences sharedPref = this.getSharedPreferences("sharedPref",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(userName,privateKey);
```
## Encryption
The application keeps messages in an encrypted form in the database.
```
String cipherMessage = Base64.getEncoder().encodeToString(encrypt(messageText, receiverPublicKey));
```
While encrypting the messages, it makes RSA encryption with the receiver's public key.
<p align="left">
<img  src="https://github.com/zahitkaya/chat-app/blob/master/images/encryptedMessages.PNG" width="70%" height="70%" >
</p>

## Decryption
After the application pulls ciphertext in the database, it decrypts it in the client and messages appear on the user interface.
```
            if(message.getReceiver().equals(mAuth.getCurrentUser().getEmail())){
                        plainText=decrypt(plainText,senderPrivateKey);
                    }
                    else {
                        plainText=decrypt(plainText,receiverPrivateKey);
                    }

```
<p align="center">
<img src="https://github.com/zahitkaya/chat-app/blob/master/images/Screenshot_1616186234.png" width="25%" height="25%" >
</p>

## Used Technologies
* Firebase Database 19.6
* Firebase Auth 20.0.3
* Toasty 1.5.0
* Lombok 1.18.16
* Java Cryptography Architecture
