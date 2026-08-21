package com.example.backend.Admin.Dto;

import com.example.backend.Cms.Entity.InternalLink;
import com.example.backend.Cms.Enums.InternalTargetType;
import com.example.backend.Cms.Enums.LinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reklama, premyera va bildirishnoma uchun umumiy havola DTO'si. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalLinkDto {

    @Builder.Default
    private LinkType linkType = LinkType.NONE;

    private String linkUrl;
    private InternalTargetType internalTargetType;
    private Long internalTargetId;

    public static InternalLinkDto from(InternalLink link) {
        if (link == null) {
            return InternalLinkDto.builder().build();
        }
        return InternalLinkDto.builder()
                .linkType(link.getLinkType() == null ? LinkType.NONE : link.getLinkType())
                .linkUrl(link.getLinkUrl())
                .internalTargetType(link.getInternalTargetType())
                .internalTargetId(link.getInternalTargetId())
                .build();
    }

    public InternalLink toEntity() {
        return InternalLink.builder()
                .linkType(linkType == null ? LinkType.NONE : linkType)
                .linkUrl(linkUrl)
                .internalTargetType(internalTargetType)
                .internalTargetId(internalTargetId)
                .build();
    }
}
