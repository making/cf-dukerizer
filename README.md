## Java + OpenCV on Cloud Foundry

```
./mvnw clean package -Plinux-x86_64 -DskipTests=true
jar -uvf target/dukerizer-0.0.1-SNAPSHOT.jar Aptfile .buildpacks
cf push
```

```
curl -v -F 'file=@face.jpg' http://dukerizer.<app domain>/dukerize.jpg > foo.jpg
```