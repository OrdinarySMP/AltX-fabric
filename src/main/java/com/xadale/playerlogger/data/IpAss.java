package com.xadale.playerlogger.data;

import com.xadale.playerlogger.core.AbstractData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an IP address association with one or more UUIDs.
 *
 * <p>The IpAss (IP Association) class maintains a relationship between a single IP address and
 * multiple unique identifiers (UUIDs), typically used to track associations between network
 * addresses and entities. The class includes mechanisms to:
 *
 * <ul>
 *   <li>Store and manage UUIDs associated with an IP address
 *   <li>Prevent duplicate UUID entries
 *   <li>Sync changes through an associated Repository
 * </ul>
 */
public class IpAss extends AbstractData<IpAss, String> {

  private String ip;
  private List<UUID> uuids;

  /**
   * Constructs an IpAss object with the specified IP address and initial UUID. The UUID list is
   * initialized with the provided UUID as its first entry.
   *
   * @param ip The IP address to associate with this object
   * @param uuid The initial UUID to add to the association list
   */
  public IpAss(String ip, UUID uuid) {
    this.ip = ip;
    this.uuids = new ArrayList<>();
    this.uuids.add(uuid);
  }

  /**
   * Returns the IP address associated with this IpAss object.
   *
   * @return The IP address string
   */
  public String getIp() {
    return ip;
  }

  /**
   * Returns a list of UUIDs associated with the IP address.
   *
   * @return A List containing all associated UUIDs
   */
  public List<UUID> getUuids() {
    return uuids;
  }

  /**
   * Adds a UUID to the association list if it doesn't already exist, and triggers a data update
   * through the associated Repository.
   *
   * @param uuid The UUID to add to the association list
   */
  public void addUUid(UUID uuid) {
    if (!this.uuids.contains(uuid)) {
      this.uuids.add(uuid);
    }
    this.getRepository().update(this);
  }
}
