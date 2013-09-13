/**
 * Copyright (c) 2009--2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.redhat.rhn.frontend.xmlrpc.activationkey.test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import redstone.xmlrpc.XmlRpcSerializer;

import com.redhat.rhn.FaultException;
import com.redhat.rhn.common.validator.ValidatorException;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.config.ConfigChannel;
import com.redhat.rhn.domain.config.ConfigChannelType;
import com.redhat.rhn.domain.rhnpackage.PackageName;
import com.redhat.rhn.domain.rhnpackage.test.PackageNameTest;
import com.redhat.rhn.domain.role.RoleFactory;
import com.redhat.rhn.domain.server.ManagedServerGroup;
import com.redhat.rhn.domain.server.ServerConstants;
import com.redhat.rhn.domain.server.ServerGroup;
import com.redhat.rhn.domain.server.ServerGroupType;
import com.redhat.rhn.domain.token.ActivationKey;
import com.redhat.rhn.domain.token.TokenPackage;
import com.redhat.rhn.domain.token.test.ActivationKeyTest;
import com.redhat.rhn.frontend.xmlrpc.InvalidChannelException;
import com.redhat.rhn.frontend.xmlrpc.MissingEntitlementException;
import com.redhat.rhn.frontend.xmlrpc.activationkey.ActivationKeyHandler;
import com.redhat.rhn.frontend.xmlrpc.serializer.ActivationKeySerializer;
import com.redhat.rhn.frontend.xmlrpc.test.BaseHandlerTestCase;
import com.redhat.rhn.manager.entitlement.EntitlementManager;
import com.redhat.rhn.manager.token.ActivationKeyManager;
import com.redhat.rhn.testing.ChannelTestUtils;
import com.redhat.rhn.testing.ConfigTestUtils;
import com.redhat.rhn.testing.ServerGroupTestUtils;
import com.redhat.rhn.testing.UserTestUtils;

public class ActivationKeyHandlerTest extends BaseHandlerTestCase {

    private ActivationKeyHandler keyHandler = new ActivationKeyHandler();
    private static final String KEY = "myexplicitkey";
    private static final String KEY_DESCRIPTION = "Test Key";
    private static final Integer KEY_USAGE_LIMIT = new Integer(0);
    private static final List<String> KEY_ENTITLEMENTS;
    static {
        KEY_ENTITLEMENTS = new LinkedList<String>();
        KEY_ENTITLEMENTS.add(EntitlementManager.PROVISIONING_ENTITLED);
        KEY_ENTITLEMENTS.add(EntitlementManager.VIRTUALIZATION_ENTITLED);
    }

    private Channel baseChannel;
    private String baseChannelLabel;

    public void setUp() throws Exception {
        super.setUp();
        baseChannel = ChannelTestUtils.createBaseChannel(admin);
        baseChannelLabel = baseChannel.getLabel();
    }

    public void testCreateWithAutogeneratedKey() throws Exception {
        String key = keyHandler.create(adminKey, "", KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.TRUE);
        assertTrue(key.length() > 0);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(key, admin);
        assertEquals(1, activationKey.getChannels().size());
        assertEquals(activationKey.getId(), admin.getOrg().getToken().getId());
        assertEquals(0, activationKey.getUsageLimit().longValue());
        assertEquals(KEY_DESCRIPTION, activationKey.getNote());

        assertEquals(3, activationKey.getEntitlements().size());
        // Created by default:
        assertTrue(keyHasEntitlement(activationKey,
                ServerConstants.getServerGroupTypeEnterpriseEntitled()));
        assertTrue(keyHasEntitlement(activationKey,
                ServerConstants.getServerGroupTypeProvisioningEntitled()));
        assertTrue(keyHasEntitlement(activationKey,
                ServerConstants.getServerGroupTypeVirtualizationEntitled()));
    }

    public void testCreateWithBlankChannelAndUnlimitedUsageLimit() throws Exception {
        List <String> ents = new ArrayList<String>(1);
        ents.add(EntitlementManager.PROVISIONING_ENTITLED);
        String key = keyHandler.create(adminKey, "", KEY_DESCRIPTION, null,
                                ents, Boolean.TRUE);
        assertTrue(key.length() > 0);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                lookupByKey(key, admin);
        assertTrue(activationKey.getChannels().isEmpty());
        assertEquals(activationKey.getId(), admin.getOrg().getToken().getId());
        assertNull(activationKey.getUsageLimit());
        assertEquals(KEY_DESCRIPTION, activationKey.getNote());
    }

    public void testCreateWithZeroUsageLimit() throws Exception {
        String key = keyHandler.create(adminKey, "", KEY_DESCRIPTION, null,
                                new Integer(0), KEY_ENTITLEMENTS, Boolean.TRUE);
        assertTrue(key.length() > 0);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
               lookupByKey(key, admin);
        assertEquals(0, activationKey.getUsageLimit().longValue());
    }

    public void testCreateWithExplicitKey() throws Exception {
        String resultingKey = keyHandler.create(adminKey, KEY, "testing",
                baseChannelLabel, new Integer(0), KEY_ENTITLEMENTS, Boolean.FALSE);
        assertFalse(KEY.equals(resultingKey));
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                lookupByKey(resultingKey, admin);
        assertFalse(activationKey.getToken().isOrgDefault());
        assertFalse(activationKey.getToken().equals(admin.getOrg().getToken()));
    }

    public void testCreateWithSameKey() throws Exception {
        keyHandler.create(adminKey, KEY, "testing",
                baseChannelLabel, new Integer(0), KEY_ENTITLEMENTS, Boolean.FALSE);
        try {
            keyHandler.create(adminKey, KEY, "testing",
                baseChannelLabel, new Integer(0), KEY_ENTITLEMENTS, Boolean.FALSE);
            fail();
        }
        catch (FaultException e) {
            // expected
        }
    }

    public void testCreateWithInvaidChannelId() throws Exception {
        try {
            keyHandler.create(adminKey, "", "testing", "NOT A CHANNEL",
                new Integer(0), KEY_ENTITLEMENTS, Boolean.FALSE);
            fail();
        }
        catch (InvalidChannelException e) {
            // expected
        }
    }

    public void testCreateWithChildChannel() throws Exception {
        Channel childChannel = ChannelTestUtils.createChildChannel(admin, baseChannel);
        String childChannelLabel = childChannel.getLabel();
        try {
            keyHandler.create(adminKey, "", "testing", childChannelLabel,
                new Integer(0), KEY_ENTITLEMENTS, Boolean.FALSE);
            fail();
        }
        catch (InvalidChannelException e) {
            // expected
        }
    }

    public void testCreateWithBaseEntitlements() {
        List<String> badEntitlements = new LinkedList<String>();
        badEntitlements.add("enterprise_entitled");
        try {
            keyHandler.create(adminKey, "", "testing", baseChannelLabel,
                    new Integer(0), badEntitlements, Boolean.FALSE);
            fail();
        }
        catch (FaultException fe) {
            // expected
        }
    }

    public void testCreateWithInvalidEntitlement() {
        List<String> badEntitlements = new LinkedList<String>();
        badEntitlements.add("monitoring_foobar");
        try {
            keyHandler.create(adminKey, "", "testing", baseChannelLabel,
                    new Integer(0), badEntitlements, Boolean.FALSE);
            fail();
        }
        catch (FaultException fe) {
            // expected
        }
    }

    public void testActivationKeyRoleEnforced() throws Exception {
        try {
            keyHandler.create(regularKey, KEY, KEY_DESCRIPTION,
                baseChannelLabel, KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
            fail();
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testUpdateEverything() throws Exception {

        String key = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.TRUE);

        Map details = new HashMap();
        String description2 = "Test Key 2";
        Channel baseChannel2 = ChannelTestUtils.createBaseChannel(admin);
        String baseChannelId2 = baseChannel2.getLabel();
        details.put("description", description2);
        details.put("base_channel_label", baseChannelId2);
        details.put("usage_limit", new Integer(15));
        details.put("universal_default", Boolean.FALSE);
        keyHandler.setDetails(adminKey, key, details);

        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                        lookupByKey(key, admin);

        assertEquals(1, activationKey.getChannels().size());
        assertEquals(baseChannel2, activationKey.getChannels().iterator().next());
        assertEquals(new Long(15), activationKey.getUsageLimit());
        assertFalse(activationKey.isUniversalDefault());
    }

    public void testSetDetailsUnlimitedUsage() throws Exception {

        String key = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.TRUE);

        Map details = new HashMap();
        details.put("usage_limit", new Integer(15)); // should be ignored
        details.put("unlimited_usage_limit", Boolean.TRUE);
        keyHandler.setDetails(adminKey, key, details);

        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                        lookupByKey(key, admin);

        assertNull(activationKey.getUsageLimit());
    }

    public void testGetDetails() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);

        Channel childChannel = ChannelTestUtils.createChildChannel(admin, baseChannel);
        String childChannelLabel = childChannel.getLabel();
        keyHandler.addChildChannels(adminKey, newKey, buildList(childChannelLabel));

        ServerGroup group = ServerGroupTestUtils.createManaged(admin);
        keyHandler.addServerGroups(adminKey, newKey,
                buildList(new Integer(group.getId().intValue())));

        PackageName newName = PackageNameTest.createTestPackageName();
        keyHandler.addPackageNames(adminKey, newKey, buildList(newName.getName()));

        PackageName newName2 = PackageNameTest.createTestPackageName();
        keyHandler.addPackageNames(adminKey, newKey, buildList(newName2.getName()));

        PackageName newName3 = PackageNameTest.createTestPackageName();
        keyHandler.addPackageNames(adminKey, newKey, buildList(newName3.getName()));

        ActivationKey key = keyHandler.getDetails(adminKey, newKey);
        Writer output = new StringWriter();
        ActivationKeySerializer serializer = new ActivationKeySerializer();
        serializer.serialize(key, output, new XmlRpcSerializer());
        String finalResult = output.toString();

        assertTrue(finalResult.indexOf(newKey) >= 0);
        assertTrue(finalResult.indexOf(KEY_DESCRIPTION) >= 0);
        assertTrue(finalResult.indexOf("<i4>" + KEY_USAGE_LIMIT + "</i4>") >= 0);
        assertTrue(finalResult.indexOf("<string>" + baseChannelLabel + "</string>") >= 0);

        assertTrue(finalResult.indexOf(newName.getName()) >= 0);
        assertTrue(finalResult.indexOf(newName2.getName()) >= 0);
        assertTrue(finalResult.indexOf(newName3.getName()) >= 0);
    }

    public void testSetAddOnEntitlement() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance()
                                                    .lookupByKey(newKey, admin);
        assertEquals(3, activationKey.getEntitlements().size());
        assertTrue(keyHasEntitlement(activationKey,
                ServerConstants.getServerGroupTypeEnterpriseEntitled()));
        assertTrue(keyHasEntitlement(activationKey,
                ServerConstants.getServerGroupTypeProvisioningEntitled()));
        assertTrue(keyHasEntitlement(activationKey,
                ServerConstants.getServerGroupTypeVirtualizationEntitled()));

        ServerGroupType monitoring = ServerConstants.getServerGroupTypeMonitoringEntitled();
        keyHandler.addEntitlements(adminKey, newKey,
                buildEntitlementsList(new String []{monitoring.getLabel()}));
        assertEquals(4, activationKey.getEntitlements().size());
        assertTrue(keyHasEntitlement(activationKey, monitoring));
    }

    private List<String> buildEntitlementsList(String [] entitlements) {
        List<String> entitlementList = new LinkedList<String>();
        for (int i = 0; i < entitlements.length; i++) {
            entitlementList.add(entitlements[i]);
        }
        return entitlementList;
    }

    public void testSetNonExistentEntitlement() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        try {
            keyHandler.addEntitlements(adminKey, newKey,
                    buildEntitlementsList(new String []{"not a real label"}));
            fail();
        }
        catch (ValidatorException e) {
            // expected
        }
    }

    public void testSetNonAddOnEntitlement() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        try {
            // Not an add-on entitlement:
            keyHandler.addEntitlements(adminKey, newKey,
                    buildEntitlementsList(new String []{"sw_mgr_entitled"}));
            fail();
        }
        catch (ValidatorException e) {
            // expected
        }
    }

    /*
     * Ensure no exception is thrown when adding an entitlement the key already has.
     */
    public void testReAddEntitlement() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(3, activationKey.getEntitlements().size());
        keyHandler.addEntitlements(adminKey, newKey,
                buildEntitlementsList(new String []{"provisioning_entitled"}));
        assertEquals(3, activationKey.getEntitlements().size());
    }

    public void testRemoveAddOnEntitements() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                           lookupByKey(newKey, admin);
        assertEquals(3, activationKey.getEntitlements().size());

        keyHandler.removeEntitlements(adminKey, newKey, KEY_ENTITLEMENTS);
        assertEquals(1, activationKey.getEntitlements().size());
    }

    /*
     * No exception should be thrown removing an entitlement the key doesn't have:
     */
    public void testRemoveUnappliedAddOnEntitements() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                        lookupByKey(newKey, admin);
        assertEquals(3, activationKey.getEntitlements().size());

        List<String> entsToRemove = new LinkedList<String>();
        entsToRemove.add("virtualization_host_platform");
        keyHandler.removeEntitlements(adminKey, newKey, entsToRemove);
        assertEquals(3, activationKey.getEntitlements().size());
    }

    public void testRemoveNonExistentAddOnEntitlement() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        try {
            keyHandler.removeEntitlements(adminKey, newKey,
                    buildEntitlementsList(new String []{"not a real entitlement"}));
            fail();
        }
        catch (ValidatorException e) {
            // expected
        }
    }

    public void testAddChildChannel() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(1, activationKey.getChannels().size());

        Channel childChannel = ChannelTestUtils.createChildChannel(admin, baseChannel);
        String childChannelLabel = childChannel.getLabel();
        keyHandler.addChildChannels(adminKey, newKey, buildList(childChannelLabel));
        assertEquals(2, activationKey.getChannels().size());
    }

    public void testAddBaseChannelAsChild() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                           lookupByKey(newKey, admin);
        assertEquals(1, activationKey.getChannels().size());

        Channel anotherBaseChan = ChannelTestUtils.createBaseChannel(admin);
        String channelLabel = anotherBaseChan.getLabel();
        try {
            keyHandler.addChildChannels(adminKey, newKey, buildList(channelLabel));
            fail();
        }
        catch (InvalidChannelException e) {
            // expected
        }
    }

    /*
     * Adding a child channel when we don't have the parent should throw an exception.
     */
    public void testAddChildChannelFromDifferentBase() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                          lookupByKey(newKey, admin);
        assertEquals(1, activationKey.getChannels().size());

        Channel anotherBaseChan = ChannelTestUtils.createBaseChannel(admin);
        Channel childChannel = ChannelTestUtils.createChildChannel(admin,
            anotherBaseChan);
        String childChannelLabel = childChannel.getLabel();
        try {
            keyHandler.addChildChannels(adminKey, newKey, buildList(childChannelLabel));
            fail();
        }
        catch (InvalidChannelException e) {
            // expected
        }
    }

    public void testRemoveChildChannel() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                        lookupByKey(newKey, admin);
        assertEquals(1, activationKey.getChannels().size());
        Channel childChannel = ChannelTestUtils.createChildChannel(admin, baseChannel);
        String childChannelLabel = childChannel.getLabel();
        keyHandler.addChildChannels(adminKey, newKey, buildList(childChannelLabel));
        assertEquals(2, activationKey.getChannels().size());

        keyHandler.removeChildChannels(adminKey, newKey, buildList(childChannelLabel));
        assertEquals(1, activationKey.getChannels().size());
    }

    public void testAddServerGroup() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getServerGroups().size());

        ServerGroup group = ServerGroupTestUtils.createManaged(admin);
        keyHandler.addServerGroups(adminKey, newKey,
                buildList(new Integer(group.getId().intValue())));
        assertEquals(1, activationKey.getServerGroups().size());
    }

    /*
     * Don't throw any exceptions when trying to re-add a server group we already have.
     */
    public void testReAddServerGroup() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getServerGroups().size());

        ServerGroup group = ServerGroupTestUtils.createManaged(admin);
        keyHandler.addServerGroups(adminKey, newKey,
                buildList(new Integer(group.getId().intValue())));
        assertEquals(1, activationKey.getServerGroups().size());

        keyHandler.addServerGroups(adminKey, newKey,
                buildList(new Integer(group.getId().intValue())));
        assertEquals(1, activationKey.getServerGroups().size());
    }

    public void testRemoveServerGroup() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getServerGroups().size());

        ServerGroup group = ServerGroupTestUtils.createManaged(admin);
        keyHandler.addServerGroups(adminKey, newKey,
                buildList(new Integer(group.getId().intValue())));
        assertEquals(1, activationKey.getServerGroups().size());

        keyHandler.removeServerGroups(adminKey, newKey,
                buildList(new Integer(group.getId().intValue())));
        assertEquals(0, activationKey.getServerGroups().size());
    }

    /*
     * Don't throw an exception when trying to remove a server group that hasn't been
     * added.
     */
    public void testRemoveServerGroupNotThere() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                           lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getServerGroups().size());
        ManagedServerGroup sg = ServerGroupTestUtils.createManaged(admin);
        keyHandler.removeServerGroups(adminKey, newKey, buildList(
                new Integer(sg.getId().intValue())));
        assertEquals(0, activationKey.getServerGroups().size());
    }

    public void testAddPackageName() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getPackages().size());

        PackageName newName = PackageNameTest.createTestPackageName();
        keyHandler.addPackageNames(adminKey, newKey, buildList(newName.getName()));
        assertEquals(1, activationKey.getPackages().size());
    }

    public void testRemovePackageName() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getPackages().size());

        PackageName newName = PackageNameTest.createTestPackageName();
        keyHandler.addPackageNames(adminKey, newKey, buildList(newName.getName()));
        assertEquals(1, activationKey.getPackages().size());

        keyHandler.removePackageNames(adminKey, newKey, buildList(newName.getName()));
        assertEquals(0, activationKey.getPackages().size());
    }

    public void testAddPackages() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        // check initial state
        assertEquals(0, activationKey.getPackages().size());

        // setup test
        List<Map<String, String>> packages = new ArrayList<Map<String, String>>();
        Map<String, String> pkg1 = new HashMap<String, String>();
        pkg1.put("name", "pkg1");
        pkg1.put("arch", "i386");
        packages.add(pkg1);

        Map<String, String> pkg2 = new HashMap<String, String>();
        pkg2.put("name", "pkg2");
        pkg2.put("arch", "");
        packages.add(pkg2);

        Map<String, String> pkg3 = new HashMap<String, String>();
        pkg3.put("name", "pkg3");
        packages.add(pkg3);

        // execute test
        int result = keyHandler.addPackages(adminKey, newKey, packages);

        // verify results
        assertEquals(1, result);
        assertEquals(3, activationKey.getPackages().size());

        String name = null, arch = null;
        boolean foundPkg1 = false, foundPkg2 = false, foundPkg3 = false;

        for (TokenPackage pkg : activationKey.getPackages()) {
            if ((pkg.getPackageName() != null) &&
                pkg.getPackageName().getName().equals("pkg1")) {

                if ((pkg.getPackageArch() != null) &&
                    pkg.getPackageArch().getLabel().equals("i386")) {

                    foundPkg1 = true;
                }
            }
            else if ((pkg.getPackageName() != null) &&
                     pkg.getPackageName().getName().equals("pkg2")) {

                if (pkg.getPackageArch() == null) {
                    foundPkg2 = true;
                }
            }
            else if ((pkg.getPackageName() != null) &&
                     pkg.getPackageName().getName().equals("pkg3")) {

                if (pkg.getPackageArch() == null) {
                    foundPkg3 = true;
                }
            }
        }
        assertTrue(foundPkg1);
        assertTrue(foundPkg2);
        assertTrue(foundPkg3);
    }

    public void testRemovePackages() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        // check initial state
        assertEquals(0, activationKey.getPackages().size());

        // setup test
        List<Map<String, String>> packages = new ArrayList<Map<String, String>>();
        Map<String, String> pkg1 = new HashMap<String, String>();
        pkg1.put("name", "pkg1");
        pkg1.put("arch", "i386");
        packages.add(pkg1);

        Map<String, String> pkg2 = new HashMap<String, String>();
        pkg2.put("name", "pkg2");
        pkg2.put("arch", "");
        packages.add(pkg2);

        Map<String, String> pkg3 = new HashMap<String, String>();
        pkg3.put("name", "pkg3");
        packages.add(pkg3);

        keyHandler.addPackages(adminKey, newKey, packages);
        assertEquals(3, activationKey.getPackages().size());

        // execute tests and verify results
        packages.clear();
        Map<String, String> unknownPkg = new HashMap<String, String>();
        unknownPkg.put("name", "unknown");
        unknownPkg.put("arch", "i386");
        packages.add(unknownPkg);
        int result = keyHandler.removePackages(adminKey, newKey, packages);
        assertEquals(3, activationKey.getPackages().size());

        packages.clear();
        packages.add(pkg2);
        result = keyHandler.removePackages(adminKey, newKey, packages);
        assertEquals(2, activationKey.getPackages().size());

        packages.clear();
        packages.add(pkg1);
        packages.add(pkg3);
        result = keyHandler.removePackages(adminKey, newKey, packages);
        assertEquals(0, activationKey.getPackages().size());
    }

    public void testAddNonExistentPackage() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                            lookupByKey(newKey, admin);
        assertEquals(0, activationKey.getPackages().size());

        keyHandler.addPackageNames(adminKey, newKey, buildList("notarealpackage"));
        // if the package name didn't exist, it should be added...
        assertEquals(1, activationKey.getPackages().size());
    }

    private boolean keyHasEntitlement(ActivationKey key, ServerGroupType entitlement) {
        boolean found = false;
        for (Iterator it = key.getEntitlements().iterator(); it.hasNext();) {
            ServerGroupType current = (ServerGroupType)it.next();
            if (current.getId() == entitlement.getId()) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void testAddPackageWithoutProvisioningEntitementFails() throws Exception
    {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, new LinkedList(), Boolean.FALSE);

        PackageName newName = PackageNameTest.createTestPackageName();
        try {
            keyHandler.addPackageNames(adminKey, newKey, buildList(newName.getName()));
            fail();
        }
        catch (MissingEntitlementException e) {
            // expected
        }
    }

    public void testRemoveProvisioningEntitementClearsPackages() throws Exception
    {
        String newKey = keyHandler.create(adminKey, KEY, KEY_DESCRIPTION, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKey activationKey = ActivationKeyManager.getInstance().
                                                        lookupByKey(newKey, admin);

        PackageName newName = PackageNameTest.createTestPackageName();
        keyHandler.addPackageNames(adminKey, newKey, buildList(newName.getName()));
        assertEquals(1, activationKey.getPackages().size());

        keyHandler.removeEntitlements(adminKey, newKey, KEY_ENTITLEMENTS);
        assertEquals(0, activationKey.getPackages().size());
    }

    /**
     * Pack the given input into a list.
     * @param input Object to pack
     * @return Single item list
     */
    private List buildList(Object input) {
        List output = new LinkedList();
        output.add(input);
        return output;
    }

    /*
     * Care not to test in a fragile manner. No idea how many activation keys might exist
     * in the database on the host we're testing against, so assume nothing but what we
     * create in the actual test.
     */
    public void testListActivationKeys() throws Exception {
        // Create a bunch of keys:
        for (int i = 0; i < 5; i++) {
            keyHandler.create(adminKey, "", KEY_DESCRIPTION + " " + i, baseChannelLabel,
                    KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        }

        List<ActivationKey> keys = keyHandler.listActivationKeys(adminKey);

        assertTrue(keys.size() >= 5);
    }

    public void testListActivatedSystems() throws Exception {
        ActivationKey key = ActivationKeyTest.createTestActivationKey(admin);
        int numServersActivated = key.getToken().getActivatedServers().size();

        Object[] servers = keyHandler.listActivatedSystems(adminKey, key.getKey());

        assertEquals(numServersActivated, servers.length);
    }

    public void testConfigChannels() throws Exception {
        String newKey = keyHandler.create(adminKey, KEY,
                        KEY_DESCRIPTION + " " + 1, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        UserTestUtils.addProvisioning(admin.getOrg());
        UserTestUtils.addUserRole(admin, RoleFactory.CONFIG_ADMIN);
        // Create a global channel
        ConfigChannel global1 = ConfigTestUtils.createConfigChannel(admin.getOrg(),
                ConfigChannelType.global());
        ConfigChannel global2 = ConfigTestUtils.createConfigChannel(admin.getOrg(),
                ConfigChannelType.global());

        ConfigChannel global3 = ConfigTestUtils.createConfigChannel(admin.getOrg(),
                ConfigChannelType.global());

        ConfigChannel global4 = ConfigTestUtils.createConfigChannel(admin.getOrg(),
                ConfigChannelType.global());

        List<ConfigChannel> configs = new LinkedList<ConfigChannel>();
        configs.add(global1);
        configs.add(global2);
        configs.add(global3);
        configs.add(global4);
        List<String> configLabels = new LinkedList<String>();
        for (ConfigChannel cc : configs) {
            configLabels.add(cc.getLabel());
        }
        List <String> keys = new LinkedList<String>();
        keys.add(newKey);

        assertEquals(1, keyHandler.setConfigChannels(adminKey, keys, configLabels));
        assertEquals(configs, keyHandler.listConfigChannels(adminKey, newKey));
        //now test remove
        //now try catch invalid ids
        try {
            configLabels.add("1111122");
            keyHandler.setConfigChannels(adminKey, keys, configLabels);
            fail("IllegalArgumentException NOT RAISED on invalid channel addition");
        }
        catch (Exception e) {
            //exception was successfully raised!
        }
        //test remove channel
        configLabels.clear();
        for (ConfigChannel cc : configs) {
            configLabels.add(cc.getLabel());
        }

        assertEquals(1,
              keyHandler.removeConfigChannels(adminKey, keys,
                                          configLabels.subList(0, 2)));
        assertEquals(configs.subList(2, configs.size()),
                    keyHandler.listConfigChannels(adminKey, newKey));

        //test add channels
        keyHandler.addConfigChannels(adminKey, keys, configLabels.subList(0, 2), true);
        assertEquals(configs, keyHandler.listConfigChannels(adminKey, newKey));

        assertEquals(1,
                keyHandler.removeConfigChannels(adminKey, keys,
                                            configLabels.subList(2, configs.size())));
        keyHandler.addConfigChannels(adminKey, keys,
                    configLabels.subList(2, configs.size()), false);
        assertEquals(configs, keyHandler.listConfigChannels(adminKey, newKey));
    }

    public void testRemove() {
        String newKey = keyHandler.create(adminKey, KEY,
                KEY_DESCRIPTION + " " + 1, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKeyManager manager = ActivationKeyManager.getInstance();
        ActivationKey activationKey = manager.lookupByKey(newKey, admin);
        assertNotNull(activationKey);
        assertEquals(1, keyHandler.delete(adminKey, newKey));
        try {
            manager.lookupByKey(newKey, admin);
            String msg = "NUll lookup failed, because this object should NOT exist!";
            fail(msg);
        }
        catch (Exception e) {
            // great!.. Exception for null lookup is controvoersial but convenient..
        }
    }

    public void testConfigDeployment() {
        String newKey = keyHandler.create(adminKey, KEY,
                KEY_DESCRIPTION + " " + 1, baseChannelLabel,
                KEY_USAGE_LIMIT, KEY_ENTITLEMENTS, Boolean.FALSE);
        ActivationKeyManager manager = ActivationKeyManager.getInstance();
        ActivationKey activationKey = manager.lookupByKey(newKey, admin);
        assertNotNull(activationKey);

        int status = keyHandler.checkConfigDeployment(adminKey, newKey);
        //deployment status disabled by default
        assertEquals(status, 0);

        keyHandler.enableConfigDeployment(adminKey, newKey);
        status = keyHandler.checkConfigDeployment(adminKey, newKey);
        assertEquals(status, 1);
        assertTrue(activationKey.getDeployConfigs());

        keyHandler.disableConfigDeployment(adminKey, newKey);
        status = keyHandler.checkConfigDeployment(adminKey, newKey);
        assertEquals(status, 0);
        assertFalse(activationKey.getDeployConfigs());

        try {
            status = keyHandler.checkConfigDeployment(adminKey, "invalidkey");
        }
        catch (Exception e) {
            // great!.. Exception received on error...
        }
    }
}
