/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.hierarchy;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.AnonymousHaxeTypeImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiMethod;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Created by ebishton on 9/4/14.
 *
 * A set of utility functions that support the HierarchyProviders.
 */
public class HaxeHierarchyUtils {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.hierarchy.HaxeHierarchyUtils");

  // EMB: Don't check this line in...
  {
    LOG.setLevel(Level.DEBUG);
  }

  private HaxeHierarchyUtils() {
    throw new NotImplementedException("Static use only.");
  }


  /**
   * Given a PSI id element, find out if it -- or one of its parents --
   * references a class, and, if so, returns the PSI element for the class.
   *
   * @param id A PSI element for an identifier (e.g. variable name).
   * @return A PSI class element, or null if not found.
   */
  @Nullable
  public static HaxeClass findReferencedClassForId(@NotNull LeafPsiElement id) {
    if (null == id) {
      return null;
    }

    PsiReference found = id.findReferenceAt(0);
    PsiElement resolved = null;
    if (found instanceof PsiMultiReference) {
      for (PsiReference ref : ((PsiMultiReference)found).getReferences()) {
        PsiElement target = ref.resolve();
        if (null != target && target instanceof PsiClass) {
          resolved = target;
          break;
        }
      }
    }
    else {
      resolved = found.resolve();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("findReferencedClassForID found " + resolved);
    }

    HaxeClass pclass = resolved instanceof HaxeClass ? (HaxeClass) resolved : null;
    return pclass;

    //PsiElement element = id.getParent();
    //while (null != element) {
    //  if (element instanceof HaxeReferenceExpression) {
    //    HaxeClass pclass = resolveClassReference((HaxeReferenceExpression) element);
    //    if (null != pclass) {
    //      return pclass;
    //    }
    //  }
    //  if (element instanceof HaxeFile) {
    //    return null;
    //  }
    //  element = element.getParent();
    //}
    //return null;
  }

  /**
   * Retrieve the list of classes implemented in the given File.
   *
   * @param psiRoot - File to search.
   * @return An array of found classes, or an empty array if none.
   */
  public static HaxeClass[] getClassList(@NotNull HaxeFile psiRoot) {

    ArrayList<HaxeClass> classes = new ArrayList<HaxeClass>();
    for (PsiElement child : psiRoot.getChildren()) {
      if (child instanceof HaxeClass) {
        classes.add((HaxeClass)child);
      }
    }
    HaxeClass[] return_type = {};
    return (classes.toArray(return_type));
  }

  /**
   * Get the PSI element for the class containing the currently focused
   * element.  Anonymous classes can be excluded if desired.
   *
   * @param context - editing context
   * @param allowAnonymous - flag to allow anonymous classes or not.
   * @return The PSI element representing the containing class.
   */
  @Nullable
  public static AbstractHaxePsiClass getContainingClass(@NotNull DataContext context, boolean allowAnonymous) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getContainingClass " + context);
    }

    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No project");
      }
      return null;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("editor " + editor);
    }
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("No file found.");
        }
        return null;
      }

      final PsiElement targetElement = TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED |
                                                                                       TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED |
                                                                                       TargetElementUtilBase.LOOKUP_ITEM_ACCEPTED);
      if (LOG.isDebugEnabled()) {
        LOG.debug("target element " + targetElement);
      }
      if (targetElement instanceof AbstractHaxePsiClass) {
        return (AbstractHaxePsiClass)targetElement;
      }

      // Haven't found it yet, walk the PSI tree toward the root.
      final int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset);
      while (element != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("context element " + element);
        }
        if (element instanceof HaxeFile) {
          // If we get to the file node, then we're outside of a class definition.
          // No need to look further.
          return null;
        }
        if (element instanceof AbstractHaxePsiClass) {
          // Keep looking if we don't allow anonymous classes.
          if (allowAnonymous || !(element instanceof AnonymousHaxeTypeImpl)) {
            return (AbstractHaxePsiClass)element;
          }
        }
        element = element.getParent();
      }

      return null;
    }
    else {
      final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
      return element instanceof AbstractHaxePsiClass ? (AbstractHaxePsiClass)element : null;
    }
  }

  /**
   * Retrieve the PSI element for the file containing the given
   * context (focus element).
   *
   * @param context - editing context
   * @return The PSI node representing the file element.
   */
  @Nullable
  public static HaxeFile getContainingFile(@NotNull DataContext context) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getContainingFile " + context);
    }

    // XXX: EMB: Can we just ask for the node at offset 0??
    PsiElement element = getPsiElement(context);
    while (element != null) {
      if (element instanceof HaxeFile) {
        return (HaxeFile)element;
      }
      element = element.getParent();
    }
    return null;
  }

  /**
   * Retrieve the PSI element for the given context (focal point).
   * Returns the leaf-node element at the exact position in the PSI.
   * This does NOT attempt to locate a higher-order PSI element as
   * {@link TargetElementUtilBase#findTargetElement} would.
   *
   * @param context - editing context
   * @return The PSI element at the caret position.
   */
  @Nullable
  public static PsiElement getPsiElement(@NotNull DataContext context) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getPsiElement " + context);
    }

    PsiElement element = null;

    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No project");
      }
      return null;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("editor " + editor);
    }
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("No file found.");
        }
        return null;
      }

      final int offset = editor.getCaretModel().getOffset();
      element = file.findElementAt(offset);
    }
    else {
      element = CommonDataKeys.PSI_ELEMENT.getData(context);
    }
    return element;
  }

  @Nullable
  public static PsiElement getReferencedElement(@NotNull DataContext context) {
    PsiElement element = null;

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (editor != null) {
      element = TargetElementUtil.findTargetElement(editor,
                                    TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED |
                                    TargetElementUtil.ELEMENT_NAME_ACCEPTED);
    }

    return element;
  }

  /**
   * Determine if there is a method that is the target of the current
   * action, and, if so, return it.
   *
   * @param context Editor context.
   * @return The PSI method if the current context points at a method,
   *         null otherwise.
   */
  @Nullable
  public static HaxePsiMethod getTargetMethod(@NotNull DataContext context) {

    final PsiElement logicalElement = HaxeHierarchyUtils.getReferencedElement(context);
    if (logicalElement == null) {
      return null;
    }

    // Apparently, the tree that referenced element is part of is NOT
    // the AST tree from the parsed file, but rather a PSI tree that
    // matches the logical structure of the language.  The parent of a
    // referenced component is always the element we want the type of.
    HaxeComponentType ctype = HaxeComponentType.typeOf(logicalElement.getParent());
    if (ctype == HaxeComponentType.METHOD) {
      // What we need to return is not the element we checked the type of,
      // nor the corresponding parsed file element.
      // Instead, we need to return the composite HaxePsiMethod class.
      HaxeComponentWithDeclarationList psiElement =
        (HaxeComponentWithDeclarationList)logicalElement.getParent();
      HaxePsiMethod psiMethod = new HaxePsiMethod(psiElement);
      return psiMethod;
    }
    return null;
  }


  /**
   * Determine the class (PSI element), if any, that is referenced by the
   * given reference expression.
   *
   * @param element A PSI reference expression.
   * @return The associated class, if any.  null if not found.
   */
  @Nullable
  public static HaxeClass resolveClassReference(@NotNull HaxeReference element) {
    HaxeClassResolveResult result = element.resolveHaxeClass();
    HaxeClass pclass = result == null ? null : result.getHaxeClass();
    return pclass;
  }

} // END class HaxeHierarchyUtils