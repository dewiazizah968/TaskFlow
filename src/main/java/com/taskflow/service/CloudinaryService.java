package com.taskflow.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper around the Cloudinary SDK. Every uploaded file (task
 * attachments and profile pictures) goes through here instead of being
 * written to the local disk, so the app stays stateless and works on
 * hosts without persistent storage.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a file to Cloudinary under the given folder (e.g.
     * "taskflow/attachments" or "taskflow/profiles").
     *
     * @return the result, containing the public HTTPS URL to store in the
     *         database and the Cloudinary public ID needed to delete it
     *         later.
     */
    public UploadResult upload(MultipartFile file, String folder) throws Exception {
        String uniqueId = UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "public_id", uniqueId,
                        // "auto" lets Cloudinary store PDFs/docs (resource_type "raw")
                        // as well as images/videos correctly.
                        "resource_type", "auto"
                )
        );

        String secureUrl = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");
        String resourceType = (String) result.get("resource_type");

        return new UploadResult(secureUrl, publicId, resourceType);
    }

    /**
     * Deletes a previously uploaded file. Safe to call even if the file
     * was already removed; failures are swallowed since this is normally
     * called as clean-up after deleting the corresponding DB row.
     */
    public void delete(String publicId, String resourceType) {
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType == null ? "image" : resourceType)
            );
        } catch (Exception ignored) {
            // Best-effort cleanup; not worth failing the request over.
        }
    }

    public record UploadResult(String secureUrl, String publicId, String resourceType) {
    }
}
