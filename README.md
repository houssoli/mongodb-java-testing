# MongoDB Java Testing Tutorial

Samples how to write integration tests for mongo db based applications using the Java client API and the flapdoodle framework with Maven.

Please feel free to have a look at [from blog] for the full tutorial.

## Map Reduce in human-readable form

Because Java is missing an elegant way to declare multiline strings I've added the map and reduce functions here:

**map**

    function () {
        this.categories.forEach(function (category) {
            emit(category, {
                count: 1
            });
        })
    };


**reduce**

    function (key, values) {
        var sum = 0;
        for (var i = 0; i < values.length; i++)
            sum += values[i].count;
    
        return {
            count: sum
        };
    }

**result**

    { "_id" : "crime" , "value" : { "count" : 1.0}}
    { "_id" : "horror" , "value" : { "count" : 2.0}}
    { "_id" : "mystery" , "value" : { "count" : 2.0}}
    { "_id" : "romance" , "value" : { "count" : 1.0}}
    { "_id" : "science" , "value" : { "count" : 2.0}}
    { "_id" : "sports" , "value" : { "count" : 1.0}}
    
## Sample usage of embedmongo-maven-plugin

```xml
<plugin>
    <groupId>com.github.joelittlejohn.embedmongo</groupId>
    <artifactId>embedmongo-maven-plugin</artifactId>
    <version>0.1.12</version>
    <executions>
        <execution>
            <id>start</id>
            <goals>
                <goal>start</goal>
            </goals>
            <configuration>
                <port>37017</port>
                <!-- optional, default 27017 -->

                <randomPort>true</randomPort>
                <!-- optional, default is false, if true allocates a random port and overrides embedmongo.port -->

                <version>2.0.4</version>
                <!-- optional, default 2.2.1 -->

                <databaseDirectory>/tmp/mongotest</databaseDirectory>
                <!-- optional, default is a new dir in java.io.tmpdir -->

                <logging>file</logging>
                <!-- optional (file|console|none), default console -->

                <logFile>${project.build.directory}/myfile.log</logFile>
                <!-- optional, can be used when logging=file, default is ./embedmongo.log -->

                <logFileEncoding>utf-8</logFileEncoding>
                <!-- optional, can be used when logging=file, default is utf-8 -->

                <proxyHost>myproxy.company.com</proxyHost>
                <!-- optional, default is none -->

                <proxyPort>8080</proxyPort>
                <!-- optional, default 80 -->

                <proxyUser>joebloggs</proxyUser>
                <!-- optional, default is none -->

                <proxyPassword>pa55w0rd</proxyPassword>
                <!-- optional, default is none -->

                <bindIp>127.0.0.1</bindIp>
                <!-- optional, default is to listen on all interfaces -->

                <downloadPath>http://internal-mongo-repo/</downloadPath>
                <!-- optional, default is http://fastdl.mongodb.org/ -->

                <skip>false</skip>
                <!-- optional, skips this plugin entirely, use on the command line like -Dembedmongo.skip -->
            </configuration>
        </execution>
        <execution>
            <id>stop</id>
            <goals>
                <goal>stop</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

----
**2013 Micha Kops / hasCode.com**

[from blog] http://www.hascode.com/
