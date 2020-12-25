@SqApi79
Feature: Smoketests

  This is for running smoketests using somewhat more complex test data.

  Scenario: Smoketest
    Given the project "smoketest_project"
    And rule "cppcheck:unusedVariable" is enabled
    And rule "cppcheck:unreadVariable" is enabled
    And rule "cppcheck:deallocDealloc" is enabled
    And rule "cppcheck:doubleFree" is enabled
    And rule "cppcheck:uninitvar" is enabled
    And rule "cppcheck:unusedFunction" is enabled
    And rule "cppcheck:missingInclude" is enabled
    When I run sonar-scanner with "-X"
    Then the analysis finishes successfully
    And the analysis in server has completed
    And the analysis log contains no error/warning messages except those matching:
      """
      .*WARN.*Unable to get a valid mac address, will use a dummy address
      .*WARN.*cannot find the sources for '#include <gtest/gtest\.h>'
      .*WARN.*cannot find the sources for '#include <iostream>'
      .*WARN.*Cannot find the file '.*component_XXX.cc', skipping
      .*WARN.*Cannot find a report for '.*'
      """
    And the following metrics have following values:
      | metric                         | value |
      | ncloc                          | 56    |
      | lines                          | 151   |
      | statements                     | 36    |
      | classes                        | 1     |
      | files                          | 8     |
      | directories                    | None  |
      | functions                      | 5     |
      | comment_lines_density          | 30    |
      | comment_lines                  | 24    |
      | duplicated_lines_density       | 55.6  |
      | duplicated_lines               | 84    |
      | duplicated_blocks              | 2     |
      | duplicated_files               | 2     |
      | complexity                     | 7     |
      | cognitive_complexity           | 2     |
      | file_complexity                | 0.9   |
      | violations                     | 12    |
      | lines_to_cover                 | 31    |
      | coverage                       | 53.8  |
      | line_coverage                  | 54.8  |
      | branch_coverage                | 50    |
      | uncovered_conditions           | 4     |
      | uncovered_lines                | 14    |
      | tests                          | 5     |
      | test_failures                  | 2     |
      | test_errors                    | 0     |
      | skipped_tests                  | 1     |
      | test_execution_time            | 159   |
      | test_success_density           | None  |
      | false_positive_issues          | 0     |
      | open_issues                    | 12    |
      | confirmed_issues               | 0     |
      | reopened_issues                | 0     |
      | code_smells                    | 6     |
      | sqale_index                    | 30    |
      | sqale_debt_ratio               | 1.8   |
      | bugs                           | 6     |
      | reliability_remediation_effort | 30    |
      | vulnerabilities                | 0     |
      | security_remediation_effort    | 0     |
      | security_hotspots              | 0     |


  Scenario: Project using Bullseye coverage, xUnit, Cppcheck, Rats, Vera++
    This test verifies that analysis is able to import Bullseye coverage reports and import custom rules reports.
    Custom rules are created using Rest API, after test ends rules are deleted.
    Bullseye reports need to be created before running the test.

    Given the project "googletest_bullseye_vs_project"
    And rule "cppcheck:unreadVariable" is enabled
    And rule "cppcheck:missingInclude" is enabled
    And rule "rats:getenv" is enabled
    And rule "vera++:T013" is enabled
    And rule "vera++:L003" is enabled
    And rule "vera++:T008" is enabled
    And rule "cpplint_legal_copyright_0" is created based on "other:CustomRuleTemplate" in repository "other"
    And rule "cpplint_build_header_guard_0" is created based on "other:CustomRuleTemplate" in repository "other"
    And rule "cpplint_whitespace_indent_2" is created based on "other:CustomRuleTemplate" in repository "other"
    And rule "cpplint_whitespace_parens_5" is created based on "other:CustomRuleTemplate" in repository "other"
    And rule "cpplint_whitespace_line_length_1" is created based on "other:CustomRuleTemplate" in repository "other"
    And rule "cpplint_tekla_custom_include_files_0" is created based on "other:CustomRuleTemplate" in repository "other"
    When I run sonar-scanner with "-X"
    Then the analysis finishes successfully
    And the analysis in server has completed
    And the analysis log contains no error/warning messages except those matching:
      """
      .*WARN.*Unable to get a valid mac address, will use a dummy address
      .*WARN.*to create a dependency with 'PathHandling/PathHandle.h'
      .*WARN.*cannot find the sources for '#include <unistd\.h>'
      .*WARN.*Cannot find the file '.*gtestmock.1.7.2.*', ignoring coverage measures
      .*WARN.*Cannot find a report for '.*'
      .*WARN.*cannot find the sources for '#include.*
      """
    And the following metrics have following values:
      | metric                   | value |
      | ncloc                    | 24    |
      | lines                    | 42    |
      | statements               | 7     |
      | classes                  | 1     |
      | files                    | 2     |
      | functions                | 3     |
      | comment_lines_density    | 0     |
      | duplicated_lines_density | 0     |
      | duplicated_lines         | 0     |
      | duplicated_blocks        | 0     |
      | duplicated_files         | 0     |
      | complexity               | 4     |
      | file_complexity          | 2.0   |
      | violations               | 19    |
      | line_coverage            | 100   |
      | branch_coverage          | 50    |
      | test_failures            | 1     |
      | test_errors              | 0     |
      | tests                    | 2     |
    And delete created rule other:cpplint_build_header_guard_0
    And delete created rule other:cpplint_legal_copyright_0
    And delete created rule other:cpplint_whitespace_indent_2
    And delete created rule other:cpplint_whitespace_parens_5
    And delete created rule other:cpplint_whitespace_line_length_1
    And delete created rule other:cpplint_tekla_custom_include_files_0
