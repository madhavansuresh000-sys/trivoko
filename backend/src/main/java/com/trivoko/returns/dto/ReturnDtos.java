package com.trivoko.returns.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.trivoko.returns.ReturnEvent;
import com.trivoko.returns.ReturnReason;
import com.trivoko.returns.ReturnRequest;
import com.trivoko.returns.ReturnStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Everything the returns API sends and receives. */
public final class ReturnDtos {

	private ReturnDtos() {
	}

	/** Customer: "Return this item". The photo (optional) must be one of our Cloudinary uploads. */
	public record RequestReturn(
			@NotNull Long orderItemId,
			@NotNull ReturnReason reason,
			@NotBlank @Size(min = 5, max = 300, message = "Tell the seller what is wrong (5-300 characters)") String note,
			@Size(max = 500) String photoUrl) {
	}

	/** One line of the return timeline. */
	public record EventView(ReturnStatus status, ReturnEvent.Actor actor, String note, LocalDateTime at) {
	}

	public record ReturnView(Long id, Long orderItemId, Long orderId, String orderNumber, String productName,
			String variantLabel, int quantity, String sellerName, ReturnStatus status, ReturnReason reason,
			String note, String photoUrl, BigDecimal refundAmount, String rejectReason, LocalDateTime createdAt,
			List<EventView> events) {

		public static ReturnView of(ReturnRequest r) {
			return new ReturnView(r.getId(), r.getOrderItemId(), r.getOrderId(), r.getOrderNumber(), r.getProductName(),
					r.getVariantLabel(), r.getQuantity(), r.getSellerName(), r.getStatus(), r.getReason(),
					r.getNote(), r.getPhotoUrl(), r.getRefundAmount(), r.getRejectReason(), r.getCreatedAt(),
					r.getEvents().stream().map(e -> new EventView(e.getStatus(), e.getActor(), e.getNote(),
							e.getCreatedAt())).toList());
		}

	}

}
