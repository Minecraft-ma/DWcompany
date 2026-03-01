package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Interface for storage providers (YAML and MySQL).
 * Defines the contract for loading and saving company data.
 */
public interface StorageProvider {

    /**
     * Initializes the storage connection/setup.
     *
     * @return true if initialization was successful
     */
    boolean initialize();

    /**
     * Shuts down the storage connection.
     */
    void shutdown();

    /**
     * Loads all companies from storage.
     *
     * @return Collection of loaded companies
     */
    Collection<Company> loadAllCompanies();

    /**
     * Saves all companies to storage.
     *
     * @param companies Collection of companies to save
     * @return true if all saved successfully
     */
    boolean saveAllCompanies(Collection<Company> companies);

    /**
     * Deletes a company from storage.
     *
     * @param companyName The name of the company to delete
     * @return true if deleted successfully
     */
    boolean deleteCompany(String companyName);

    /**
     * Checks if storage is connected/available.
     *
     * @return true if storage is ready
     */
    boolean isConnected();

    /**
     * Gets the storage type name.
     *
     * @return Storage type (YAML or MySQL)
     */
    String getStorageType();

    /**
     * Gets the last save timestamp.
     *
     * @return Last save time in milliseconds
     */
    long getLastSaveTime();

    /**
     * Validates data integrity.
     *
     * @return true if data is valid
     */
    boolean validateData();
}
