# Copyright (c) 2021 SUSE LLC
# Licensed under the terms of the MIT license.

Feature: Build OS image for SLES 12 SP4

  Scenario: Create an JeOS6 profile with activation key
    Given I am authorized as "admin" with password "admin"
    When I follow the left menu "Images > Profiles"
    And I follow "Create"
    And I enter "jeos6" as "label"
    And I select "Kiwi" from "imageType"
    And I select "1-sle12sp4_minion_key" from "activationKey"
    And I enter "https://github.com/SUSE/manager-build-profiles#master:OSImage/POS_Image-JeOS6" as "path"
    And I click on "create-btn"

  Scenario: Set record of proxy at SLES 12 SP4 buildhost hosts file if avahi is used
    When I add proxy record into hosts file on "sle12sp4_buildhost" if avahi is used

  Scenario: Login as Kiwi image administrator and build an JeOS6 image
    Given I am authorized as "admin" with password "admin"
    When I follow the left menu "Images > Build"
    And I select "jeos6" from "profileId"
    And I select the hostname of "sle12sp4_buildhost" from "buildHostId"
    And I click on "submit-btn"

  Scenario: Check the JeOS6 image built as Kiwi image administrator
    Given I am on the Systems overview page of this "sle12sp4_buildhost"
    Then I should see a "[OS Image Build Host]" text
    When I wait until the image build "jeos6" is completed
    And I am on the image store of the kiwi image for organization "1"

  Scenario: Move JeOS6 image to the branch server
    When I manually install the "image-sync" formula on the server
    And I enable repositories before installing branch server
    And I synchronize all Salt dynamic modules on "proxy"
    And I apply state "image-sync" to "proxy"
