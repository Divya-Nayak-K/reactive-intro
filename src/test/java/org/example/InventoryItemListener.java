package org.example;

import java.util.List;

public interface InventoryItemListener {
    void onInventoryUpdate(List<InventoryItem> updates);
    void onInventoryCheckInComplete();
}