#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiServer.h>
#include <WiFiUDP.h>

#include <Adafruit_VC0706.h>
#include <SoftwareSerial.h>         

//Cam Variables

SoftwareSerial cameraconnection = SoftwareSerial(3, 2);
Adafruit_VC0706 cam = Adafruit_VC0706(&cameraconnection);
boolean canStart = false;

//WiFi Variables
char ssid[] = "Sitecom";     //  your network SSID (name)
char pass[] = "P@ssw0rd";    // your network password
int status = WL_IDLE_STATUS;     // the Wifi radio's status

//Tcp Variables

unsigned int localPort = 34568;      // local port to listen on
WiFiServer server(localPort);
WiFiClient client;

//Car Variables

int pinPosterioreDx = 9;//define I1-3 port 
int pinPosterioreSx = 8;//define I2-4 port 
int speedpinPosteriore = 11;//define EB(PWM speed regulation)port 

int pinAnterioreDx = 4;//define I1-1 port 
int pinAnterioreSx = 5;//define I2-2 port 
int speedpinAnteriore = 6;//define EA(PWM speed regulation)port 

//int FotocameraSx = 12;
//int FotocameraDx = 13;
//int speedpinFotocamera = 10;

int velocitaPosteriore = 250;//MAX 250 MIN 100
int velocitaAnteriore = 250;//MAX 250 MIN 100
int velocitaFotocamera = 250;//MAX 250 MIN 100

//END OF VARIABLES

void setup()
{
  InitializeCar();
  InitializeCamera();
  //LfNetworks();
  
  ConnectTo();
  
  Serial.println();
  Serial.println("CAM and CAR READY");
  // listen for incoming clients
  do
    client = server.available();
  while (!client);
  
  Serial.println("qualcuno connesso");
}

void loop()
{
   String s = "";

   char x = -1;
   boolean esci = false;
    
   while(!esci)
   {
      x = client.read();

      if(x != -1 && x != '.')
        s += x;
      else
        esci = true;
   }
   
   if(s == "stp")
    StopMotore();
   else if(s == "fwd")
    Accelera();
   else if(s == "bck")
    Retro();
   else if(s == "ctr")
    Centro();
   else if(s == "sx")
    Sinistra(); 
   else if(s == "dx")
    Destra();
   else if(s == "Shoot")
     ShootPhotos();   
}

//WiFi METHODS

void ConnectTo()
{  
  boolean shieldOn = true;
  
    // check for the presence of the shield:
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present");
    shieldOn = false;
  }

  if(shieldOn)
  {
      // attempt to connect using WPA2 encryption:
    Serial.println("Attempting to connect to WPA network...");
    status = WiFi.begin(ssid, pass);
  
    // if you're not connected, stop here:
    if ( status != WL_CONNECTED) {
      Serial.println("Couldn't get a wifi connection");
    }
    // if you are connected, print out info about the connection:
    else {
      server.begin();
      Serial.print("Connected to wifi. My address:");
      IPAddress myAddress = WiFi.localIP();
      Serial.println(myAddress);
    }
  }
}

void LfNetworks()
{
  Serial.println("** Scan Networks **");
  byte numSsid = WiFi.scanNetworks();

  // print the list of networks seen:
  Serial.print("number of available networks:");
  Serial.println(numSsid);

  // print the network number and name for each network found:
  for (int thisNet = 0; thisNet<numSsid; thisNet++) {
    Serial.print(thisNet);
    Serial.print(") ");
    Serial.print(WiFi.SSID(thisNet));
    Serial.print("\tSignal: ");
    Serial.print(WiFi.RSSI(thisNet));
    Serial.print(" dBm");
    Serial.print("\tEncryption: ");
    Serial.println(WiFi.encryptionType(thisNet));
  }
}

//CAM METHODS

void InitializeCamera()
{
 Serial.begin(38400); //ex 9600
 Serial.println("VC0706 Camera snapshot test");

 // Try to locate the camera
 if (cam.begin(38400))
 {
   Serial.println("Camera Found:");
   canStart = true;
   
    // Print out the camera version information (optional)
    
   char *reply = cam.getVersion();

   if (reply == 0)
   {
       Serial.print("Failed to get version");
   }
   else
   {
       Serial.println("-----------------");
       Serial.print(reply);
       Serial.println("-----------------");
   }

   delay(3000); // You should wait 2-3 seconds here

    //cam.setImageSize(VC0706_640x480);        // biggest
    //cam.setImageSize(VC0706_320x240);        // medium
   cam.setImageSize(VC0706_160x120);          // small

    // You can read the size back from the camera (optional, but maybe useful?)
//    uint8_t imgsize = cam.getImageSize();
//    Serial.print("Image size: ");
//    if (imgsize == VC0706_640x480) Serial.println("640x480");
//    if (imgsize == VC0706_320x240) Serial.println("320x240");
//    if (imgsize == VC0706_160x120) Serial.println("160x120");
 }
 else
 {
    Serial.println("No camera found?");
   canStart = false;
 }
}

void ShootPhotos()
{
  if(canStart)
 {
   Serial.println();
   //Serial.println("Snap in 3 secs..."); //forse si possono togliere i 3 sec
   //delay(3000);

   if(! cam.takePicture())
       Serial.println("Failed to snap!");
   else
       Serial.println("Picture taken!");
       
   uint16_t jpglen = cam.frameLength(); 
   //server.print(jpglen);
   
   Serial.print("Jpglen: ");
   Serial.println(jpglen);
   server.println(jpglen, DEC);
   
   String s = "";
  
   while(s != "stxadv")
   {
     char x = 0;
     boolean esci = false;
      
     while(!esci)
     {
        x = client.read();
  
        if(x != -1)
          s += x;
        else
          esci = true;
     }
   }
    
   while (jpglen > 0)
   {
       // read 32 bytes at a time;
       uint8_t *buffer;
       uint8_t bytesToRead = min(32, jpglen); // change 32 to 64 for a speedup but may not work with all setups!
       buffer = cam.readPicture(bytesToRead);
       //Serial.write(buffer, bytesToRead);
       jpglen -= bytesToRead;
       int inviati = 0;
       
       server.write(buffer, bytesToRead);      
   }
   
   cam.resumeVideo();
   Serial.println();
   Serial.println();
   Serial.println("END");
 }
}


//CAR METHODS


void InitializeCar()
{
    pinMode(pinPosterioreDx,OUTPUT);//define this port as output 
    pinMode(pinPosterioreSx,OUTPUT); 
    pinMode(speedpinPosteriore,OUTPUT);
    analogWrite(speedpinPosteriore, velocitaPosteriore);//input a value to set the speed 
    
    pinMode(pinAnterioreDx,OUTPUT);//define this port as output 
    pinMode(pinAnterioreSx,OUTPUT); 
    pinMode(speedpinAnteriore,OUTPUT);
    analogWrite(speedpinAnteriore, velocitaAnteriore);//input a value to set the speed 
    
//    pinMode(FotocameraSx,OUTPUT);//define this port as output 
//    pinMode(FotocameraDx,OUTPUT); 
//    pinMode(speedpinFotocamera,OUTPUT);
//    analogWrite(velocitaFotocamera, velocitaFotocamera);//input a value to set the speed 
}

void Centro()
{
  digitalWrite(pinAnterioreDx,HIGH);// DC motor rotates clockwise 
  digitalWrite(pinAnterioreSx,HIGH); 
}

void Destra()
{
  digitalWrite(pinAnterioreDx,LOW);// DC motor rotates clockwise 
  digitalWrite(pinAnterioreSx,HIGH); 
}

void Sinistra()
{
  digitalWrite(pinAnterioreDx,HIGH);// DC motor rotates anticlockwise 
  digitalWrite(pinAnterioreSx,LOW); 
}

void Retro()
{
    digitalWrite(pinPosterioreDx,LOW);// DC motor rotates clockwise 
    digitalWrite(pinPosterioreSx,HIGH); 
}

void Accelera()
{
    digitalWrite(pinPosterioreDx,HIGH);// DC motor rotates clockwise 
    digitalWrite(pinPosterioreSx,LOW); 
}

void StopMotore()
{
    digitalWrite(pinPosterioreDx,HIGH);// DC motor stop rotating 
    digitalWrite(pinPosterioreSx,HIGH); 
}
