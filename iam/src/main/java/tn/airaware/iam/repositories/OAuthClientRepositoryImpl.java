package tn.airaware.iam.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.airaware.iam.config.IamDatabase;
import tn.airaware.iam.entities.OAuthClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * OAuth Client Repository - Direct MongoDB implementation
 * Bypasses JNoSQL to avoid DocumentTemplate ambiguity issues
 */
@ApplicationScoped
public class OAuthClientRepositoryImpl implements OAuthClientRepository {

    private static final Logger LOGGER = Logger.getLogger(OAuthClientRepositoryImpl.class.getName());
    private static final String COLLECTION_NAME = "oauth_clients";

    @Inject
    @IamDatabase
    private MongoDatabase database;

    private MongoCollection<Document> getCollection() {
        return database.getCollection(COLLECTION_NAME);
    }

    @Override
    public Optional<OAuthClient> findByClientId(String clientId) {
        Document doc = getCollection().find(Filters.eq("client_id", clientId)).first();
        return Optional.ofNullable(documentToOAuthClient(doc));
    }

    @Override
    public Optional<OAuthClient> findByOrganizationName(String organizationName) {
        Document doc = getCollection().find(Filters.eq("organization_name", organizationName)).first();
        return Optional.ofNullable(documentToOAuthClient(doc));
    }

    @Override
    public List<OAuthClient> findByActive(boolean active) {
        List<OAuthClient> clients = new ArrayList<>();
        for (Document doc : getCollection().find(Filters.eq("active", active))) {
            OAuthClient client = documentToOAuthClient(doc);
            if (client != null) {
                clients.add(client);
            }
        }
        return clients;
    }

    @Override
    public OAuthClient save(OAuthClient client) {
        if (client.getId() == null || client.getId().isEmpty()) {
            client.setId(UUID.randomUUID().toString());
        }
        
        Document doc = oauthClientToDocument(client);
        getCollection().replaceOne(
                Filters.eq("_id", client.getId()),
                doc,
                new ReplaceOptions().upsert(true)
        );
        
        LOGGER.info("Saved OAuth client: " + client.getClientId());
        return client;
    }

    @Override
    public Optional<OAuthClient> findById(String id) {
        Document doc = getCollection().find(Filters.eq("_id", id)).first();
        return Optional.ofNullable(documentToOAuthClient(doc));
    }

    @Override
    public void delete(OAuthClient client) {
        getCollection().deleteOne(Filters.eq("_id", client.getId()));
        LOGGER.info("Deleted OAuth client: " + client.getClientId());
    }

    @Override
    public void deleteById(String id) {
        getCollection().deleteOne(Filters.eq("_id", id));
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

    private Document oauthClientToDocument(OAuthClient client) {
        Document doc = new Document();
        doc.put("_id", client.getId());
        doc.put("client_id", client.getClientId());
        doc.put("client_secret", client.getClientSecret());
        doc.put("redirect_uri", client.getRedirectUri());
        doc.put("allowed_roles", client.getAllowedRoles());
        doc.put("required_scopes", client.getRequiredScopes());
        doc.put("supported_grant_types", client.getSupportedGrantTypes());
        doc.put("active", client.isActive());
        doc.put("organization_name", client.getOrganizationName());
        return doc;
    }

    private OAuthClient documentToOAuthClient(Document doc) {
        if (doc == null) return null;
        
        OAuthClient client = new OAuthClient();
        client.setId(doc.getString("_id"));
        client.setClientId(doc.getString("client_id"));
        client.setClientSecret(doc.getString("client_secret"));
        client.setRedirectUri(doc.getString("redirect_uri"));
        client.setAllowedRoles(doc.getLong("allowed_roles"));
        client.setRequiredScopes(doc.getString("required_scopes"));
        client.setSupportedGrantTypes(doc.getString("supported_grant_types"));
        client.setActive(doc.getBoolean("active", true));
        client.setOrganizationName(doc.getString("organization_name"));
        return client;
    }
}