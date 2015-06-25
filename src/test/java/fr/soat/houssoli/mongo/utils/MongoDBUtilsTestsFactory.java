package fr.soat.houssoli.mongo.utils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor.Noop;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * This class encapsulates everything that would be needed to do embedded MongoDB testing.
 * 
 * @author lhoussou
 *
 */
public class MongoDBUtilsTestsFactory {
    protected Logger LOGGER = LoggerFactory.getLogger(getClass().getName());

    // By default - localhost
    public static final String DEFAULT_LOCAL_HOST = "localhost";

    // By default - port 27017
    public static final int DEFAULT_PORT = com.mongodb.DBPort.PORT;

    // By default - download http://downloads.mongodb.org/ or https://www.mongodb.org/dl/
    public static final String DOWNLOAD_EMBEDMONGO_TARGET_PATH = "http://downloads.mongodb.org/";

    // By default - download into <user.home>/.embedmongo
    public static final String EMBEDMONGO_ARTIFACT_STORE_PATH = System.getProperty("user.home") + File.separatorChar + ".embedmongo";

    // Data by default in Java Project - target/embedmongo-db
    public static final String MONGO_DATA_PATH_IN_PRJ_TARGET_DEFAULT = "target" + File.separatorChar + "embedmongo-db";
    // Data in User Home - <user.home>/data/embedmongo-db
    public static final String MONGO_DATA_PATH_IN_USER_HOME = System.getProperty("user.home") + "data" + File.separatorChar
            + "embedmongo-db";

    private final String host;
    private final int port;
    private final String mongoDataPath;
    private final String downloadPath;
    private final String embedMongoArtifactStorePath;

    private final MongodExecutable mongodExecutable;

    private final MongodProcess mongodProcess;

    /**
     * 
     * @author lhoussou
     *
     */
    public static enum VersionEnum implements IFeatureAwareVersion {
        V2_6_LATEST("v2.6-latest", Feature.SYNC_DELAY), // v2.6-latest
        V2_6_10("2.6.10", Feature.SYNC_DELAY); // "2.6.10"

        private final String specificVersion;
        private final EnumSet<Feature> features;

        VersionEnum(String vName, Feature... features) {
            this.specificVersion = vName;
            this.features = Feature.asSet(features);
        }

        @Override
        public String asInDownloadPath() {
            return specificVersion;
        }

        @Override
        public boolean enabled(Feature feature) {
            return features.contains(feature);
        }

        @Override
        public String toString() {
            return "Version{" + specificVersion + '}';
        }
    }

    public static MongoDBUtilsTestsFactory with(final IFeatureAwareVersion version) throws IOException {
        return new MongoDBUtilsTestsFactory(version);
    }

    public static MongoDBUtilsTestsFactory with(final IFeatureAwareVersion version, final String downloadPath,
            final String embedMongoArtifactStorePath) throws IOException {
        return new MongoDBUtilsTestsFactory(version, downloadPath, embedMongoArtifactStorePath);
    }

    public static MongoDBUtilsTestsFactory with(final IFeatureAwareVersion version, final String host, final int port,
            final String downloadPath, final String embedMongoArtifactStorePath, final String mongoDataPath) throws IOException {
        return new MongoDBUtilsTestsFactory(version, downloadPath, embedMongoArtifactStorePath);
    }

    /**
     * Create the testing utility using the latest production version of MongoDB.
     * 
     * @throws IOException
     */
    public MongoDBUtilsTestsFactory() throws IOException {
        this(Version.Main.PRODUCTION);
    }

    /**
     * Create the testing utility using the specified version of MongoDB. Starts in-memory Mongo DB process.
     * 
     * @param version
     *            version of MongoDB.
     * @throws IOException
     */
    public MongoDBUtilsTestsFactory(final IFeatureAwareVersion version) throws IOException {
        this(version, DEFAULT_LOCAL_HOST, DEFAULT_PORT, DOWNLOAD_EMBEDMONGO_TARGET_PATH, EMBEDMONGO_ARTIFACT_STORE_PATH,
                MONGO_DATA_PATH_IN_PRJ_TARGET_DEFAULT);
    }

    /**
     * Create the testing utility using the specified version of MongoDB. Starts in-memory Mongo DB process.
     * 
     * @param version
     *            version of MongoDB.
     * @param downloadPath
     * @param embedMongoArtifactStorePath
     * @throws IOException
     */
    public MongoDBUtilsTestsFactory(final IFeatureAwareVersion version, final String downloadPath, final String embedMongoArtifactStorePath)
            throws IOException {
        this(version, DEFAULT_LOCAL_HOST, DEFAULT_PORT, downloadPath, embedMongoArtifactStorePath, MONGO_DATA_PATH_IN_PRJ_TARGET_DEFAULT);
    }

    /**
     * Create the testing utility using the specified version of MongoDB. Starts in-memory Mongo DB process.
     * 
     * @param version
     *            version of MongoDB.
     * @param host
     * @param port
     * @param downloadPath
     * @param embedMongoArtifactStorePath
     * @param mongoDataPath
     * @throws IOException
     */
    public MongoDBUtilsTestsFactory(final IFeatureAwareVersion version, final String host, final int port, final String downloadPath,
            final String embedMongoArtifactStorePath, final String mongoDataPath) throws IOException {
        this.host = host;
        this.port = port;
        this.downloadPath = downloadPath;
        this.embedMongoArtifactStorePath = embedMongoArtifactStorePath;
        this.mongoDataPath = mongoDataPath;

        IRuntimeConfig runtimeConfig = createRuntimeConfig(getDownloadPath(), getEmbedMongoArtifactStorePath()).build();

        final MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);

        // MongodConfigBuilder
        final MongodConfigBuilder mongodConfigBuilder = createMongodConfigBuilder(version);
        mongodConfigBuilder.cmdOptions(new MongoCmdOptionsBuilder().enableAuth(false).build());
        mongodConfigBuilder.setParameter("authenticationMechanisms", "MONGODB-CR");

        // TODO mongodConfigBuilder.setParameter Key="authenticationMechanisms", value="MONGODB-CR" | "SCRAM-SHA-1" | "PLAIN"
        // Net net = null;
        // TODO mongodConfigBuilder.net(net);
        // IMongoProcessListener processListener = null;
        // TODO mongodConfigBuilder.processListener(processListener);

        // MongodConfig
        final IMongodConfig mongodConfig = mongodConfigBuilder.build();

        mongodExecutable = runtime.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
    }

    /**
     * 
     * @param downloadPath
     * @param embeddedMongoArtifactStorePath
     * 
     * @return
     */
    protected de.flapdoodle.embed.process.config.RuntimeConfigBuilder createRuntimeConfig(final String downloadPath,
            final String embeddedMongoArtifactStorePath) {
        Command command = Command.MongoD;

        ITempNaming executableNaming = "true".equals("true") ? new de.flapdoodle.embed.process.extract.UUIDTempNaming()
                : new de.flapdoodle.embed.process.extract.UserTempNaming();

        de.flapdoodle.embed.process.config.store.DownloadConfigBuilder downloadConfigBuilder = new DownloadConfigBuilder()
                .defaultsForCommand(command);
        if (StringUtils.isNotEmpty(downloadPath)) {
            downloadConfigBuilder.downloadPath(downloadPath);
        }

        if (StringUtils.isNotEmpty(embeddedMongoArtifactStorePath)) {
            IDirectory artifactStorePath = new FixedPath(embeddedMongoArtifactStorePath);
            downloadConfigBuilder.artifactStorePath(artifactStorePath);
        }

        ICommandLinePostProcessor commandLinePostProcessor = new AdditionalCommandLinePostProcessor();
        de.flapdoodle.embed.process.config.RuntimeConfigBuilder runtimeConfig = new de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder()
                .defaultsWithLogger(command, LOGGER)
                .commandLinePostProcessor(commandLinePostProcessor)
                .artifactStore(
                new ArtifactStoreBuilder().defaults(command).download(downloadConfigBuilder).executableNaming(executableNaming));
        return runtimeConfig;
    }

    /**
     * Additional command line arguments
     * 
     * @author houssoli
     *
     */
    class AdditionalCommandLinePostProcessor extends Noop {
        @Override
        public List<String> process(Distribution distribution, List<String> args) {
            List<String> processArgs = super.process(distribution, args);

            // TODO : allow more additional command line arguments
            // processArgs.add("--noauth");
            // processArgs.add("--setParameter authenticationMechanisms=PLAIN");

            return processArgs;
        }
    }

    /**
     * 
     * @param version
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    protected MongodConfigBuilder createMongodConfigBuilder(final IFeatureAwareVersion version) throws UnknownHostException, IOException {
        return createMongodConfigBuilder(version, getMongoDataPath(), getPort());
    }

    /**
     * 
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    protected MongodConfigBuilder createMongodConfigBuilder() throws UnknownHostException, IOException {
        return createMongodConfigBuilder(Version.Main.PRODUCTION, getMongoDataPath(), getPort());
    }

    /**
     * Creates a new MongodConfigBuilder
     * 
     * @param version
     * @param mongoDataPath
     * @param port
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    protected MongodConfigBuilder createMongodConfigBuilder(final IFeatureAwareVersion version, final String mongoDataPath, final int port)
            throws UnknownHostException, IOException {
        // Custom database directory
        Storage replication = new Storage(mongoDataPath, null, 0);

        return new MongodConfigBuilder().version(version).replication(replication).net(new Net(port, Network.localhostIsIPv6()));
    }

    /**
     * 
     * @param version
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    protected IMongodConfig newMongodConfig(final IFeatureAwareVersion version) throws UnknownHostException, IOException {
        return new MongodConfigBuilder().version(version).build();
    }

    /**
     * Creates a new MongoClient connection.
     * 
     * @throws MongoException
     * @throws UnknownHostException
     */
    public MongoClient createMongoClient() throws UnknownHostException, MongoException {
        return new MongoClient(new ServerAddress(mongodProcess.getConfig().net().getServerAddress(), mongodProcess.getConfig().net()
                .getPort()));
    }

    /**
     * Creates a new MongoClient connection.
     * 
     * @param credentialList
     * 
     * @throws MongoException
     * @throws UnknownHostException
     */
    public MongoClient createMongoClient(List<MongoCredential> credentialList) throws UnknownHostException, MongoException {
        return new MongoClient(new ServerAddress(mongodProcess.getConfig().net().getServerAddress(), mongodProcess.getConfig().net()
                .getPort()), credentialList);
    }

    /**
     * Creates a new DB with unique name for connection.
     */
    public DB newDB(final Mongo mongo) {
        return mongo.getDB(UUID.randomUUID().toString());
    }

    /**
     * Gets a DB with dbname for connection.
     */
    public DB getDB(final Mongo mongo, String dbname) {
        return mongo.getDB(dbname);
    }

    /**
     * Cleans up the resources created by the utility.
     */
    public void shutdown() {
        mongodProcess.stop();
        mongodExecutable.stop();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getMongoDataPath() {
        return mongoDataPath;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    private String getEmbedMongoArtifactStorePath() {
        return embedMongoArtifactStorePath;
    }

}
