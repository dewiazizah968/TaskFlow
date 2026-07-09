package com.taskflow.service;

import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    private static final String CLOUDINARY_FOLDER = "taskflow/profiles";

    public User uploadProfileImage(User user, MultipartFile file) throws Exception {

        // Clean up the old picture on Cloudinary (if any) before uploading the new one.
        if (user.getProfileImagePublicId() != null) {
            cloudinaryService.delete(user.getProfileImagePublicId(), "image");
        }

        CloudinaryService.UploadResult uploaded = cloudinaryService.upload(file, CLOUDINARY_FOLDER);

        user.setProfileImagePath(uploaded.secureUrl());
        user.setProfileImagePublicId(uploaded.publicId());

        return userRepository.save(user);
    }
}
