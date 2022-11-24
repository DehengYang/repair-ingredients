from pyutils.utils import Dir_util,File_util, List_util
import os, re

cur_dir = Dir_util.get_cur_dir(__file__)
donor_dir = os.path.join(cur_dir, "../results/Donor Code/")
log_dir = os.path.join(donor_dir, "logs")
result_dir = os.path.join(donor_dir, "results")

defect4j_id_txt = os.path.join(cur_dir, "defects4j.txt")
redundant_bugs_txt = os.path.join(cur_dir, "redundant_bugs.txt")
intrinsic_bugs_txt = os.path.join(cur_dir, "intrinsic.txt")

bug_ids = list(set(File_util.read_file_to_list_strip(defect4j_id_txt)))
redundant_bugs = list(set(File_util.read_file_to_list_strip(redundant_bugs_txt)))
intrinsic_bugs = list(set(File_util.read_file_to_list_strip(intrinsic_bugs_txt)))
uniq = list(set(List_util.get_uniq_in_src(bug_ids, redundant_bugs)))
print(len(bug_ids), len(redundant_bugs), len(uniq))
List_util.print_list(uniq)
exit()



def main():
    # read bug id
    # bug_ids = File_util.read_file_to_list_strip(defect4j_id_txt)
    for bug_id in bug_ids:
        # if bug_id != "Mockito_33":
        #     continue
        if bug_id in intrinsic_bugs: # should skip!
            continue
        proj, id = bug_id.split("_")
        # log dir
        cur_log_dir = os.path.join(log_dir, proj, id)
        if os.path.exists(cur_log_dir):
            total_chunks = get_total_chunks(cur_log_dir)
            # print(proj, id, total_chunks)
        
        # result dir
        cur_result_file = os.path.join(result_dir, proj, f"{proj}_{id}_donor.txt")
        if os.path.exists(cur_result_file):
            redundant_dict = parse_donor_result(cur_result_file)
            is_redundant = "no"
            if len(redundant_dict.keys()) == total_chunks:
                is_redundant = 'yes'
            elif len(redundant_dict.keys()) == 0:
                is_redundant = 'no'
            else:
                is_redundant = 'partial'
            same_scope = is_same_scope(redundant_dict.values())
            final_scope = get_largest_scope(redundant_dict.values())
            print(f"{bug_id}, {total_chunks}, {is_redundant}, {same_scope}, {final_scope}")

def is_same_scope(scope_list):
    if len(set(scope_list)) == 1:
        return True
    return False

def get_total_chunks(cur_log_dir):
    cnt = 0
    for file in os.listdir(cur_log_dir):
        if file.endswith("_fixed.log"):
            cnt+= 1
    return cnt

def parse_donor_result(cur_result_file):
    redundant_dict = {}
    lines = File_util.read_file_to_list_strip(cur_result_file)
    key = "-1"
    for line in lines:
        # chunk1 : org.jfree.data.general.DatasetUtilities:1249-1249
        matches = re.findall(r"chunk(.*?) : .*", line)
        if len(matches) != 0:
            if key != "-1": # set scope
                redundant_dict[key] = get_smallest_scope(redundant_dict[key])
            key = matches[0]
            redundant_dict[key] = []
        else:
            if line.startswith("fix ingredient"):
                if "(SameMethod)" in line:
                    redundant_dict[key].append('method')
                elif "(SameFile)"  in line:
                    redundant_dict[key].append('file')
                elif "(SamePackage)" in line:
                    redundant_dict[key].append('package')
                else:
                    redundant_dict[key].append('program')
    if key != "-1": # set scope
        redundant_dict[key] = get_smallest_scope(redundant_dict[key])
    # print(redundant_dict)
    return redundant_dict

def get_smallest_scope(scope_list):
    scope = ""
    if "method" in scope_list:
        scope = "method"
    elif "file" in scope_list:
        scope = "file"
    elif "package" in scope_list:
        scope = "package"
    else:
        scope = 'program'
    return scope

def get_largest_scope(scope_list):
    scope = ""
    if "program" in scope_list:
        scope = "program"
    elif "package" in scope_list:
        scope = "package"
    elif "file" in scope_list:
        scope = "file"
    else:
        scope = 'method'
    return scope

if __name__ == "__main__":
    main()