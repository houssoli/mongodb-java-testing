# MongoDB Java Testing Tutorial

Samples how to write integration tests for mongo db based applications using the Java client API and the flapdoodle framework with Maven.

Please feel free to have a look at [my blog] for the full tutorial.

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
    

----

**2013 Micha Kops / hasCode.com**

   [my blog]:http://www.hascode.com/