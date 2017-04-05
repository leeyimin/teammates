package teammates.client.scripts;

import teammates.client.remoteapi.RemoteApiClient;
import teammates.client.scripts.util.DataMigrationForSanitizedDataHelper;
import teammates.client.scripts.util.LoopHelper;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.logic.core.ProfilesLogic;
import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.storage.api.ProfilesDb;

import java.io.IOException;
import java.util.List;

/**
 * Script to desanitize content of {@link StudentProfileAttributes} if it is sanitized.
 * Fields {@link StudentProfileAttributes#shortName}, {@link StudentProfileAttributes#email},
 * {@link StudentProfileAttributes#institute}, {@link StudentProfileAttributes#nationality},
 * {@link StudentProfileAttributes#gender}, {@link StudentProfileAttributes#moreInfo}
 * are no longer sanitized before saving and these fields are expected to be in their unsanitized form.
 * This script desanitizes these fields of exisiting StudentProfileAttributes if they are sanitized so that
 * all profiles will have unsanitized values in these fields.
 */
public class DataMigrationForSanitizedDataInStudentProfileAttributes extends RemoteApiClient {
    private static final boolean isPreview = false;
    private ProfilesDb profilesDb = new ProfilesDb();
    private ProfilesLogic profilesLogic = ProfilesLogic.inst();

    public static void main(String[] args) throws IOException {
        new DataMigrationForSanitizedDataInStudentProfileAttributes().doOperationRemotely();
    }

    @Override
    protected void doOperation() {
        List<StudentProfileAttributes> allStudentProfiles = profilesLogic.getAllStudentProfiles();
        int numberOfAffectedProfiles = 0;
        int numberOfUpdatedProfiles = 0;
        LoopHelper loopHelper = new LoopHelper(100, "student profiles processed.");
        println("Running data migration for sanitization on student profiles...");
        println("Preview: " + isPreview);
        for (StudentProfileAttributes profile : allStudentProfiles) {
            loopHelper.recordLoop();
            boolean isProfileSanitized = isProfileSanitized(profile);
            if (!isProfileSanitized) {
                // skip the update if profile is not sanitized
                continue;
            }
            numberOfAffectedProfiles++;
            try {
                desanitizeAndUpdateProfile(profile);
                numberOfUpdatedProfiles++;
            } catch (InvalidParametersException | EntityDoesNotExistException e) {
                println("Problem sanitizing profile with google id " + profile.googleId);
                println(e.getMessage());
            }
        }
        println("Total number of profiles: " + loopHelper.getCount());
        println("Number of affected profiles: " + numberOfAffectedProfiles);
        println("Number of updated profiles: " + numberOfUpdatedProfiles);
    }

    /**
     * Desanitizes the fields of {@code profile} and updates it in the database.
     */
    private void desanitizeAndUpdateProfile(StudentProfileAttributes profile)
            throws InvalidParametersException, EntityDoesNotExistException {

        profile.shortName = getDesanitizedIfSanitized(profile.shortName);
        profile.email = getDesanitizedIfSanitized(profile.email);
        profile.institute = getDesanitizedIfSanitized(profile.institute);
        profile.nationality = getDesanitizedIfSanitized(profile.nationality);
        profile.gender = getDesanitizedIfSanitized(profile.gender);
        profile.moreInfo = getDesanitizedIfSanitized(profile.moreInfo);

        if (!profile.isValid()) {
            throw new InvalidParametersException(profile.getInvalidityInfo());
        }

        if (!isPreview) {
            profilesDb.updateStudentProfile(profile);
        }
    }

    /**
     * Returns true if any field in {@code profile} is sanitized.
     */
    private boolean isProfileSanitized(StudentProfileAttributes profile) {
        return isSanitizedHtml(profile.shortName) || isSanitizedHtml(profile.email)
                || isSanitizedHtml(profile.institute) || isSanitizedHtml(profile.nationality)
                || isSanitizedHtml(profile.gender) || isSanitizedHtml(profile.moreInfo);
    }

    /**
     * Prints the {@code string} on system output, followed by a newline.
     */
    private void println(String string) {
        System.out.println(string);
    }

    private boolean isSanitizedHtml(String string) {
        return DataMigrationForSanitizedDataHelper.isSanitizedHtml(string);
    }

    private String getDesanitizedIfSanitized(String string) {
        return DataMigrationForSanitizedDataHelper.getDesanitizedIfSanitized(string);
    }
}
