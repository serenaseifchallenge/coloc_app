package com.coloc.back.dto;

import java.util.List;

public record BalanceResponse(List<BalanceItemResponse> balances, List<TransferResponse> suggestedTransfers) {
}
