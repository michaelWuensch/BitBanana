#!/usr/bin/env python3

import os

# Define folders
script_dir = os.path.dirname(os.path.abspath(__file__))
extracted_apks_dir = os.path.join(script_dir, "extracted_apks")
FOLDER1 = os.path.join(extracted_apks_dir, "built-apks")
FOLDER2 = os.path.join(extracted_apks_dir, "playstore-apks")

# Define ingore list
IGNORE_LIST = [ "META-INF",
        "stamp-cert-sha256",
        "unknown",
        "AndroidManifest.xml",
        "apktool.yml",
        "splits0.xml"]

def files_are_identical(file1, file2, chunk_size=4096):
    """
    Compare two files by reading their bytes. Return True if they match,
    or False otherwise.
    """
    # First compare file size to avoid reading if sizes differ
    if os.path.getsize(file1) != os.path.getsize(file2):
        return False

    with open(file1, "rb") as f1, open(file2, "rb") as f2:
        while True:
            chunk1 = f1.read(chunk_size)
            chunk2 = f2.read(chunk_size)
            if chunk1 != chunk2:
                return False
            # If we've reached end of both files simultaneously, they match
            if not chunk1:  # implies chunk2 is also empty here
                return True

def compare_directories(dir1, dir2, relative_path=""):
    """
    Recursively compare the contents of dir1 and dir2.
    Return a list of difference messages.
    """
    differences = []

    # Absolute paths for the directories we're comparing at this level
    abs_dir1 = os.path.join(dir1, relative_path)
    abs_dir2 = os.path.join(dir2, relative_path)

    try:
        items1 = set(os.listdir(abs_dir1))
        items2 = set(os.listdir(abs_dir2))
    except FileNotFoundError as e:
        # If either directory doesn't exist, mark them as differences
        differences.append(f"ERROR: {e}")
        return differences

    # Apply ignore list
    items1 = {i for i in items1 if i not in IGNORE_LIST}
    items2 = {i for i in items2 if i not in IGNORE_LIST}

    # Union of both sets of items
    all_items = items1.union(items2)

    for item in all_items:
        item_path1 = os.path.join(abs_dir1, item)
        item_path2 = os.path.join(abs_dir2, item)

        # Also compute paths relative to script_dir
        rel_item_path1 = os.path.relpath(item_path1, script_dir)
        rel_item_path2 = os.path.relpath(item_path2, script_dir)
        rel_dir1 = os.path.relpath(dir1, script_dir)
        rel_dir2 = os.path.relpath(dir2, script_dir)

        # Check presence in each folder
        in_dir1 = item in items1
        in_dir2 = item in items2

        if in_dir1 and not in_dir2:
            differences.append(f"ONLY IN {rel_dir1}: {item}")
        elif in_dir2 and not in_dir1:
            differences.append(f"ONLY IN {rel_dir2}: {item}")
        else:
            # Item is in both folders
            is_dir1 = os.path.isdir(item_path1)
            is_dir2 = os.path.isdir(item_path2)

            # Compare directory-vs-file mismatch
            if is_dir1 and not is_dir2:
                differences.append(f"TYPE MISMATCH: {rel_item_path1} is a directory, {rel_item_path2} is a file")
            elif not is_dir1 and is_dir2:
                differences.append(f"TYPE MISMATCH: {rel_item_path1} is a file, {rel_item_path2} is a directory")
            else:
                # If both are directories, recurse
                if is_dir1 and is_dir2:
                    differences.extend(compare_directories(dir1, dir2, os.path.join(relative_path, item)))
                else:
                    # Both are files: compare byte by byte
                    if not files_are_identical(item_path1, item_path2):
                        differences.append(f"DIFF: {rel_item_path1} and {rel_item_path2}")

    return differences

def main():
    # Clean up paths
    folder1 = os.path.abspath(FOLDER1)
    folder2 = os.path.abspath(FOLDER2)

    if not os.path.isdir(folder1) or not os.path.isdir(folder2):
        print("ERROR: One or both specified paths are not valid directories.")
        return

    diffs = compare_directories(folder1, folder2)
    if not diffs:
        print("No differences found.")
    else:
        print("Differences:")
        for diff in diffs:
            print(diff)

if __name__ == "__main__":
    main()
