#!/usr/bin/env python3
"""
iOS Build Cycle Fix Script
Resolves Xcode build cycle by reordering build phases and adjusting dependencies
"""

import re
import shutil
import os
from datetime import datetime

def backup_project_file(project_path):
    """Create a backup of the project file"""
    backup_path = f"{project_path}.backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    shutil.copy2(project_path, backup_path)
    print(f"✅ Backup created: {backup_path}")
    return backup_path

def fix_build_phases_order(project_path):
    """Fix the build phases order to resolve circular dependency"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    # Find the Runner target build phases section
    runner_target_pattern = r'(97C146ED1CF9000F007C117D /\* Runner \*/ = \{[^}]*buildPhases = \()[^)]*(\);)'
    
    # The correct order should be:
    # 1. Check Pods Manifest.lock
    # 2. Run Script
    # 3. Sources
    # 4. Frameworks  
    # 5. Resources
    # 6. Thin Binary
    # 7. Embed Pods Frameworks
    # 8. Embed Frameworks
    # 9. Embed ExtensionKit Extensions
    # 10. Embed Foundation Extensions (MOVED TO END)
    
    correct_build_phases = """
				F30F350B4F3E4244B6963535 /* [CP] Check Pods Manifest.lock */,
				9740EEB61CF901F6004384FC /* Run Script */,
				97C146EA1CF9000F007C117D /* Sources */,
				97C146EB1CF9000F007C117D /* Frameworks */,
				97C146EC1CF9000F007C117D /* Resources */,
				3B06AD1E1E4923F5004D2608 /* Thin Binary */,
				3C8803CBD61C2F6C4AC4075F /* [CP] Embed Pods Frameworks */,
				9705A1C41CF9048500538489 /* Embed Frameworks */,
				3032BDA32E5F5C3D000CF4B1 /* Embed ExtensionKit Extensions */,
				3032BDD72E5F606D000CF4B1 /* Embed Foundation Extensions */,
			"""
    
    def replace_build_phases(match):
        return match.group(1) + correct_build_phases + match.group(2)
    
    # Apply the fix
    new_content = re.sub(runner_target_pattern, replace_build_phases, content, flags=re.DOTALL)
    
    if new_content != content:
        with open(project_path, 'w') as f:
            f.write(new_content)
        print("✅ Build phases order fixed")
        return True
    else:
        print("⚠️ No changes needed in build phases")
        return False

def add_dependency_attributes(project_path):
    """Add proper dependency attributes to prevent cycles"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    # Find the Embed Foundation Extensions build phase
    embed_phase_pattern = r'(3032BDD72E5F606D000CF4B1 /\* Embed Foundation Extensions \*/ = \{[^}]*files = \([^)]*\);[^}]*)(runOnlyForDeploymentPostprocessing = 0;)'
    
    def add_attributes(match):
        # Add dependency analysis attribute to prevent running during dependency analysis
        return match.group(1) + 'runOnlyForDeploymentPostprocessing = 1;'
    
    new_content = re.sub(embed_phase_pattern, add_attributes, content, flags=re.DOTALL)
    
    # Also modify the copy files build phase to run only for deployment
    copy_files_pattern = r'(3032BDD12E5F606D000CF4B1 /\* KeyboardExtension\.appex in Embed Foundation Extensions \*/ = \{[^}]*)(settings = \{ATTRIBUTES = \(RemoveHeadersOnCopy, \); \}; )'
    
    def modify_copy_settings(match):
        return match.group(1) + 'settings = {ATTRIBUTES = (RemoveHeadersOnCopy, ); CODE_SIGN_ON_COPY = YES; }; '
    
    new_content = re.sub(copy_files_pattern, modify_copy_settings, new_content)
    
    if new_content != content:
        with open(project_path, 'w') as f:
            f.write(new_content)
        print("✅ Dependency attributes added")
        return True
    else:
        print("⚠️ No changes needed in dependency attributes")
        return False

def fix_target_dependencies(project_path):
    """Ensure proper target dependencies without cycles"""
    
    with open(project_path, 'r') as f:
        content = f.read()
    
    # The KeyboardExtension should not depend on Runner
    # Runner should depend on KeyboardExtension for embedding
    
    # Find KeyboardExtension target dependencies
    keyboard_deps_pattern = r'(3032BDC92E5F606D000CF4B1 /\* KeyboardExtension \*/ = \{[^}]*dependencies = \()[^)]*(\);)'
    
    def clear_keyboard_deps(match):
        # KeyboardExtension should have no dependencies to break the cycle
        return match.group(1) + '\n			' + match.group(2)
    
    new_content = re.sub(keyboard_deps_pattern, clear_keyboard_deps, content, flags=re.DOTALL)
    
    if new_content != content:
        with open(project_path, 'w') as f:
            f.write(new_content)
        print("✅ Target dependencies fixed")
        return True
    else:
        print("⚠️ No changes needed in target dependencies")
        return False

def main():
    """Main function to fix iOS build cycle"""
    
    project_path = "/Users/kalyan/Ml_project/ai_keyboard/ios/Runner.xcodeproj/project.pbxproj"
    
    print("🔧 iOS Build Cycle Fix Script")
    print("=" * 50)
    
    # Check if project file exists
    if not os.path.exists(project_path):
        print(f"❌ Project file not found: {project_path}")
        return False
    
    # Create backup
    backup_path = backup_project_file(project_path)
    
    try:
        # Apply fixes
        changes_made = False
        
        print("\n1. Fixing build phases order...")
        if fix_build_phases_order(project_path):
            changes_made = True
            
        print("\n2. Adding dependency attributes...")
        if add_dependency_attributes(project_path):
            changes_made = True
            
        print("\n3. Fixing target dependencies...")
        if fix_target_dependencies(project_path):
            changes_made = True
        
        if changes_made:
            print("\n✅ BUILD CYCLE FIXES APPLIED SUCCESSFULLY!")
            print("\nNext steps:")
            print("1. Open Xcode and clean build folder (Cmd+Shift+K)")
            print("2. Try building the project again")
            print("3. If issues persist, restore from backup:")
            print(f"   cp {backup_path} {project_path}")
        else:
            print("\n⚠️ No changes were needed")
            
        return True
        
    except Exception as e:
        print(f"\n❌ Error occurred: {e}")
        print(f"Restoring from backup: {backup_path}")
        shutil.copy2(backup_path, project_path)
        return False

if __name__ == "__main__":
    main()
