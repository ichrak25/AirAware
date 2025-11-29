package tn.airaware.iam.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.airaware.core.entities.Identity;
import tn.airaware.iam.config.IamDatabase;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Identity Repository - Direct MongoDB implementation
 * Bypasses JNoSQL to avoid DocumentTemplate ambiguity issues
 */
@ApplicationScoped
public class IdentityRepositoryImpl implements IdentityRepository {

    private static final Logger LOGGER = Logger.getLogger(IdentityRepositoryImpl.class.getName());
    private static final String COLLECTION_NAME = "identities";

    @Inject
    @IamDatabase
    private MongoDatabase database;

    private MongoCollection<Document> getCollection() {
        return database.getCollection(COLLECTION_NAME);
    }

    @Override
    public Optional<Identity> findByEmail(String email) {
        Document doc = getCollection().find(Filters.eq("email", email)).first();
        return Optional.ofNullable(documentToIdentity(doc));
    }

    @Override
    public Optional<Identity> findByUsername(String username) {
        Document doc = getCollection().find(Filters.eq("username", username)).first();
        return Optional.ofNullable(documentToIdentity(doc));
    }

    @Override
    public Identity save(Identity identity) {
        if (identity.getId() == null || identity.getId().isEmpty()) {
            identity.setId(UUID.randomUUID().toString());
        }
        
        Document doc = identityToDocument(identity);
        getCollection().replaceOne(
                Filters.eq("_id", identity.getId()),
                doc,
                new ReplaceOptions().upsert(true)
        );
        
        LOGGER.info("Saved identity: " + identity.getUsername());
        return identity;
    }

    @Override
    public Optional<Identity> findById(String id) {
        Document doc = getCollection().find(Filters.eq("_id", id)).first();
        return Optional.ofNullable(documentToIdentity(doc));
    }

    @Override
    public void delete(Identity identity) {
        getCollection().deleteOne(Filters.eq("_id", identity.getId()));
        LOGGER.info("Deleted identity: " + identity.getUsername());
    }

    @Override
    public void deleteById(String id) {
        getCollection().deleteOne(Filters.eq("_id", id));
        LOGGER.info("Deleted identity with ID: " + id);
    }

    @Override
    public boolean existsById(String id) {
        return getCollection().countDocuments(Filters.eq("_id", id)) > 0;
    }

    @Override
    public long count() {
        return getCollection().countDocuments();
    }

    // ==================== Conversion Methods ====================

    private Document identityToDocument(Identity identity) {
        Document doc = new Document();
        doc.put("_id", identity.getId());
        doc.put("username", identity.getUsername());
        doc.put("email", identity.getEmail());
        doc.put("password", identity.getPassword());
        doc.put("roles", identity.getRoles());
        doc.put("scopes", identity.getScopes());
        doc.put("accountActivated", identity.isAccountActivated());
        
        if (identity.getCreationDate() != null) {
            doc.put("creationDate", identity.getCreationDate().toEpochMilli());
        }
        
        return doc;
    }

    private Identity documentToIdentity(Document doc) {
        if (doc == null) return null;
        
        Identity identity = new Identity();
        identity.setId(doc.getString("_id"));
        identity.setUsername(doc.getString("username"));
        identity.setEmail(doc.getString("email"));
        identity.setPassword(doc.getString("password"));
        identity.setRoles(doc.getLong("roles"));
        identity.setScopes(doc.getString("scopes"));
        identity.setAccountActivated(doc.getBoolean("accountActivated", false));
        
        Long creationDateMillis = doc.getLong("creationDate");
        if (creationDateMillis != null) {
            identity.setCreationDate(Instant.ofEpochMilli(creationDateMillis));
        }
        
        return identity;
    }
}